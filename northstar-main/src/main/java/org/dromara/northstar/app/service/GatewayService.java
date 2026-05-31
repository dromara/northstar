package org.dromara.northstar.app.service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.account.AccountManager;
import org.dromara.northstar.account.GatewayManager;
import org.dromara.northstar.account.TradeAccount;
import org.dromara.northstar.app.restful.SseStreamController;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.common.constant.GatewayUsage;
import org.dromara.northstar.common.exception.NoSuchGatewayException;
import org.dromara.northstar.common.exception.OperationException;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.GatewayVO;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.data.IAccountRepository;
import org.dromara.northstar.data.IGatewayRepository;
import org.dromara.northstar.data.IModuleRepository;
import org.dromara.northstar.gateway.Gateway;
import org.dromara.northstar.gateway.GatewayFactory;
import org.dromara.northstar.gateway.IMarketCenter;
import org.dromara.northstar.gateway.MarketGateway;
import org.dromara.northstar.gateway.TradeGateway;
import org.dromara.northstar.gateway.sim.trade.SimTradeGateway;
import org.dromara.northstar.gateway.sim.trade.SimAccountRemote;
import org.dromara.northstar.meta.GatewayMetaProvider;
import org.dromara.northstar.support.utils.NamingValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Order(1000)
@Service
public class GatewayService implements CommandLineRunner {
	
	@Autowired
	private GatewayManager gatewayMgr;
	@Autowired
	private GatewayMetaProvider gatewayMetaProvider;
	@Autowired
	private IMarketCenter mktCenter;
	@Autowired
	private IGatewayRepository gatewayRepo;
	@Autowired
	private IModuleRepository moduleRepo;
	@Autowired
	private IAccountRepository accountRepo;
	@Autowired
	private AccountManager accountMgr;
	@Autowired
	private SseStreamController sseController;

	public GatewayVO createGateway(GatewayDescription gd) {
		if (Objects.nonNull(gatewayMgr.get(Identifier.of(gd.getGatewayId())))) {
			throw new OperationException("error.duplicated_gateway",gd.getGatewayId());
		}
		if (!NamingValidator.isValid(gd.getGatewayId())) {
			throw new OperationException("error.illegalCharater",gd.getGatewayId());
		}
		Gateway newGateway = doCreateGateway(gd);
		doSaveGatewayDescription(gd);
		sseController.sendGatewayStatus();
		return toVO(newGateway);
	}
	
	private void doSaveGatewayDescription(GatewayDescription gatewayDescription) {
		if (null == gatewayDescription.getSettings()) {
			throw new OperationException("error.no_empty_gw_settings");
		}
		gatewayRepo.save(gatewayDescription);
	}

	private Gateway doCreateGateway(GatewayDescription gatewayDescription) {
		log.info("创建网关[{}]", gatewayDescription.getGatewayId());
		Gateway gateway = null;
		GatewayFactory factory = gatewayMetaProvider.getFactory(gatewayDescription.getChannelType());
		gateway = factory.newInstance(gatewayDescription);
		gatewayMgr.add(Identifier.of(gateway.getDescription().getGatewayId()), gateway);
		if (gatewayDescription.getGatewayUsage() == GatewayUsage.TRADE) {
			TradeAccount account = new TradeAccount((TradeGateway)gateway);
			accountMgr.add(Identifier.of(account.getId()), account);
		}
		if (gatewayDescription.getGatewayUsage() == GatewayUsage.MARKET_DATA
				&& gateway instanceof MarketGateway mktGateway) {
			mktCenter.addGateway(mktGateway);
		}
		if (gatewayDescription.isAutoConnect()) {
			gateway.connect();
		}
		return gateway;
	}

	public GatewayVO updateGateway(GatewayDescription gd) {
		Gateway gateway = gatewayMgr.get(Identifier.of(gd.getGatewayId()));
		if (gateway == null) {
			throw new NoSuchGatewayException(gd.getGatewayId());
		}
		if (gateway.getDescription().getGatewayUsage() != gd.getGatewayUsage()) {
			throw new OperationException("error.not_matched_usage");
		}
		if (gateway.getDescription().getGatewayUsage() == GatewayUsage.TRADE
				&& !gateway.getDescription().getBindedMktGatewayId().equals(gd.getBindedMktGatewayId())) {
			checkModuleBinding(gateway.getDescription().getGatewayId());
		}

		log.info("更新网关[{}]", gd.getGatewayId());
		// 更新网关不能简单地用新的网关对象替换掉旧的网关对象。因为不排除个别网关的初始化成本比较高，比如CTP网关；又或者个别网关中包含状态信息，比如SIM网关的账户余额
		// 直接粗暴地替换并不符合更新的语意
		GatewayDescription gd0 = gatewayMetaProvider.getFactory(gd.getChannelType()).enhance(gd);
		doSaveGatewayDescription(gd);
		gateway.setDescription(gd0);
		
		if (gd.isAutoConnect()) {
			gateway.connect();
		}
		sseController.sendGatewayStatus();
		return toVO(gateway);
	}

	private void checkModuleBinding(String accountGatewayId) {
		Set<String> bindingModuleNames = new HashSet<>();
		moduleRepo.findAllSettings().forEach(md -> {
			Set<String> bindedAccounts = md.getBindedAccounts().stream().collect(Collectors.toSet());
			if(bindedAccounts.contains(accountGatewayId)) {
				bindingModuleNames.add(md.getName());
			}
		});
		if(!bindingModuleNames.isEmpty()) {
			throw new OperationException("error.binded_by_modules");
		}
	}

	public List<GatewayVO> findAllGateway() {
		return gatewayMgr.findAll().stream().map(this::toVO).toList();
	}

	public GatewayVO toVO(Gateway gateway) {
		boolean isActive = false;
		if (gateway instanceof MarketGateway mktGateway) {
			isActive = mktGateway.isActive();
		}
		return GatewayVO.builder().description(gateway.getDescription()).connection(gateway.getConnection())
				.active(isActive).build();
	}

	@Transactional
	public void removeGateway(String gatewayId) {
		log.info("移除网关[{}]", gatewayId);
		Gateway gateway = null;
		Identifier id = Identifier.of(gatewayId);
		if (gatewayMgr.contains(id)) {
			gateway = gatewayMgr.get(id);
		} else {
			throw new NoSuchGatewayException(gatewayId);
		}
		if (gateway.getConnection() == ConnectionState.CONNECTED) {
			throw new OperationException("非断开状态的网关不能删除");
		}
		GatewayDescription gd = gatewayRepo.findById(gatewayId);
		if (gd.getGatewayUsage() == GatewayUsage.MARKET_DATA) {
			for (GatewayDescription gwd : gatewayRepo.findAll()) {
				if (StringUtils.equals(gwd.getBindedMktGatewayId(), gatewayId)) {
					throw new OperationException("error.binded_gateway_not_removable", gatewayId);
				}
			}
		} else {
			for (ModuleDescription md : moduleRepo.findAllSettings()) {
				for (String accountId : md.getBindedAccounts()) {
					if (StringUtils.equals(accountId, gatewayId)) {
						throw new OperationException("error.binded_gateway_not_removable", gatewayId);
					}
				}
			}
		}
		gatewayMgr.remove(id);
		gatewayRepo.deleteById(gatewayId);
		if (gd.getChannelType() == ChannelType.SIM) {
			accountRepo.removeAccounts(gatewayId);
			accountRepo.removeHoldingTrades(gatewayId);
			accountRepo.removeDealRecords(gatewayId);
		}
		
		sseController.sendGatewayStatus();
	}

	public void connect(String gatewayId) {
		log.info("连接网关[{}]", gatewayId);
		Gateway gateway = gatewayMgr.get(Identifier.of(gatewayId));
		if (Objects.isNull(gateway)) {
			throw new NoSuchGatewayException(gatewayId);
		}
		gateway.connect();
		sseController.sendGatewayStatus();
	}

	public void disconnect(String gatewayId) {
		log.info("断开网关[{}]", gatewayId);
		Gateway gateway = gatewayMgr.get(Identifier.of(gatewayId));
		if (Objects.isNull(gateway)) {
			throw new NoSuchGatewayException(gatewayId);
		}
		gateway.disconnect();
		sseController.sendGatewayStatus();
	}

	public double moneyIO(String gatewayId, int amount) {
		SimTradeGateway gateway = (SimTradeGateway) gatewayMgr.get(Identifier.of(gatewayId));
		if (Objects.isNull(gateway)) {
			throw new NoSuchGatewayException(gatewayId);
		}
		if (amount != 0) {
			log.info("模拟账户[{}]，{}金：{}", gatewayId, amount > 0 ? "入" : "出", Math.abs(amount));
		}
		
		SimAccountRemote account = gateway.getRemoteAccount();
		account.moneyIO(amount);
		sseController.sendGatewayStatus();
		return account.accountBalance();
	}

	@Override
	public void run(String... args) throws Exception {
		log.info("初始化行情网关");
		gatewayRepo.findAll().stream().filter(gd -> gd.getGatewayUsage() == GatewayUsage.MARKET_DATA).forEach(gd -> {
			try {
				doCreateGateway(gd);
			} catch (Exception e) {
				log.error("网关创建失败：{}", e);
			}
		});
		log.info("初始化交易网关");
		gatewayRepo.findAll().stream().filter(gd -> gd.getGatewayUsage() == GatewayUsage.TRADE).forEach(gd -> {
			try {
				doCreateGateway(gd);
			} catch (Exception e) {
				log.error("网关创建失败：{}", e);
			}
		});
	}
}
