package org.dromara.northstar.web.service;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.account.AccountManager;
import org.dromara.northstar.account.GatewayManager;
import org.dromara.northstar.account.TradeAccount;
import org.dromara.northstar.common.IGatewayService;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.common.constant.GatewayUsage;
import org.dromara.northstar.common.exception.NoSuchElementException;
import org.dromara.northstar.common.model.ComponentField;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.ModuleAccountDescription;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.data.IGatewayRepository;
import org.dromara.northstar.data.IModuleRepository;
import org.dromara.northstar.data.IPlaybackRuntimeRepository;
import org.dromara.northstar.data.ISimAccountRepository;
import org.dromara.northstar.gateway.Gateway;
import org.dromara.northstar.gateway.GatewayFactory;
import org.dromara.northstar.gateway.GatewayMetaProvider;
import org.dromara.northstar.gateway.IMarketCenter;
import org.dromara.northstar.gateway.MarketGateway;
import org.dromara.northstar.gateway.TradeGateway;
import org.dromara.northstar.gateway.sim.trade.SimTradeGateway;
import org.dromara.northstar.support.utils.CodecUtils;
import org.dromara.northstar.web.PostLoadAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import lombok.extern.slf4j.Slf4j;

/**
 * 网关服务
 * 注意：GatewaySetting为了防止数据库被攻破，因此对其做了对称加密，并且会在部署的机器上写入一段随机密码。
 * 只有在代码、数据库、服务器随机密码同时被攻破时，才有可能对GatewaySetting信息进行解码
 * @author KevinHuangwl
 *
 */
@Slf4j
public class GatewayService implements IGatewayService, PostLoadAware {
	@Autowired
	private GatewayManager gatewayMgr;
	@Autowired
	private GatewayMetaProvider gatewayMetaProvider;
	@Autowired
	private GatewayMetaProvider metaProvider;
	@Autowired
	private IMarketCenter mktCenter;
	@Autowired
	private IGatewayRepository gatewayRepo;
	@Autowired
	private ISimAccountRepository simAccRepo;
	@Autowired
	private IPlaybackRuntimeRepository playbackRtRepo;
	@Autowired
	private IModuleRepository moduleRepo;
	@Autowired
	private AccountManager accountMgr;
	
	/**
	 * 创建网关
	 * @return
	 * @throws Exception 
	 */
	@Override
	public boolean createGateway(GatewayDescription gatewayDescription) {
		log.info("创建网关[{}]", gatewayDescription.getGatewayId());
		doSaveGatewayDescription(gatewayDescription);
		doCreateGateway(gatewayDescription);
		if(gatewayDescription.isAutoConnect()) 
			connect(gatewayDescription.getGatewayId());
		return true;
	}
	
	private void doCreateGateway(GatewayDescription gatewayDescription) {
		Gateway gateway = null;
		GatewayFactory factory = metaProvider.getFactory(gatewayDescription.getChannelType());
		gateway = factory.newInstance(gatewayDescription);
		gatewayMgr.add(gateway);
		if(gatewayDescription.getGatewayUsage() == GatewayUsage.TRADE) {
			MarketGateway mktGateway = (MarketGateway) gatewayMgr.get(Identifier.of(gatewayDescription.getBindedMktGatewayId()));
			TradeGateway tdGateway = (TradeGateway) gateway;
			TradeAccount account = new TradeAccount(mktGateway, tdGateway, gatewayDescription);
			accountMgr.add(account);
		}
		if(gatewayDescription.getGatewayUsage() == GatewayUsage.MARKET_DATA && gateway instanceof MarketGateway mktGateway) {
			mktCenter.addGateway(mktGateway);
		}
	}
	
	private void doSaveGatewayDescription(GatewayDescription gatewayDescription) {
		if(null == gatewayDescription.getSettings()) {
			throw new IllegalArgumentException("网关设置不能为空");
		}
		Object settings = gatewayDescription.getSettings();
		String srcSettings = JSON.toJSONString(settings);
		gatewayDescription.setSettings(CodecUtils.encrypt(srcSettings));
		try {			
			gatewayRepo.insert(gatewayDescription);
		} catch(DuplicateKeyException e) {
			throw new IllegalStateException("已存在同名网关，不能重复创建", e);
		}
		gatewayDescription.setSettings(settings);
	}
	
	/**
	 * 更新网关
	 * @return
	 * @throws Exception 
	 */
	@Override
	public boolean updateGateway(GatewayDescription gatewayDescription) {
		log.info("更新网关[{}]", gatewayDescription.getGatewayId());
		doDeleteGateway(gatewayDescription.getGatewayId());
		doSaveGatewayDescription(gatewayDescription);
		// 先删除旧的，再重新创建新的
		doCreateGateway(gatewayDescription);
		if(gatewayDescription.isAutoConnect()) 
			connect(gatewayDescription.getGatewayId());
		return true;
	}
	
	/**
	 * 移除网关
	 * @return
	 */
	@Override
	public boolean deleteGateway(String gatewayId) {
		log.info("移除网关[{}]", gatewayId);
		Gateway gateway = null;
		Identifier id = Identifier.of(gatewayId);
		if(gatewayMgr.contains(id)) {
			gateway = gatewayMgr.get(id);
		} else {
			throw new NoSuchElementException("没有该网关记录：" +  gatewayId);
		}
		if(gateway.getConnectionState() == ConnectionState.CONNECTED) {
			throw new IllegalStateException("非断开状态的网关不能删除");
		}
		GatewayDescription gd = gatewayRepo.findById(gatewayId);
		if(gd.getGatewayUsage() == GatewayUsage.MARKET_DATA) {			
			for(GatewayDescription gwd : gatewayRepo.findAll()) {
				if(StringUtils.equals(gwd.getBindedMktGatewayId(), gatewayId)) {
					throw new IllegalStateException("仍有账户网关与本行情网关存在绑定关系，请先解除绑定！");
				}
			}
		} else {
			for(ModuleDescription md : moduleRepo.findAllSettings()) {
				for(ModuleAccountDescription mad : md.getModuleAccountSettingsDescription()) {
					if(StringUtils.equals(mad.getAccountGatewayId(), gatewayId)) {
						throw new IllegalStateException("仍有模组与本账户网关存在绑定关系，请先解除绑定！");
					}
				}
			}
		}
		boolean flag = doDeleteGateway(gatewayId);
		if(gd.getChannelType() == ChannelType.SIM)
			simAccRepo.deleteById(gatewayId);
		if(gd.getChannelType() == ChannelType.PLAYBACK) 
			playbackRtRepo.deleteById(gatewayId);
		return flag;
	}
	
	private boolean doDeleteGateway(String gatewayId) {
		Identifier id = Identifier.of(gatewayId);
		gatewayMgr.remove(id);
		gatewayRepo.deleteById(gatewayId);
		return true;
	}
	
	/**
	 * 查询所有网关配置
	 * @return
	 * @throws Exception 
	 */
	public List<GatewayDescription> findAllGatewayDescription() {
		return gatewayMgr.allGateways().stream().map(Gateway::gatewayDescription).toList();
	}
	
	/**
	 * 查询网关配置
	 * @param gatewayId
	 * @return
	 */
	public GatewayDescription findGatewayDescription(String gatewayId) {
		return gatewayMgr.allGateways().stream()
					.filter(gw -> StringUtils.equals(gatewayId, gw.gatewayId()))
					.findAny()
					.orElseThrow()
					.gatewayDescription();
	}
	
	/**
	 * 查询所有行情网关
	 * @return
	 * @throws Exception 
	 */
	public List<GatewayDescription> findAllMarketGatewayDescription() {
		return gatewayMgr.marketGateways().stream().map(MarketGateway::gatewayDescription).toList();
	}
	
	/**
	 * 查询所有交易网关
	 * @return
	 * @throws Exception 
	 */
	public List<GatewayDescription> findAllTraderGatewayDescription() {
		return gatewayMgr.tradeGateways().stream().map(TradeGateway::gatewayDescription).toList();
	}
	
	/**
	 * 连接网关
	 * @return
	 */
	@Override
	public boolean connect(String gatewayId) {
		log.info("连接网关[{}]", gatewayId);
		Gateway gateway = gatewayMgr.get(Identifier.of(gatewayId));
		if(Objects.isNull(gateway)) {
			throw new NoSuchElementException("没有该网关记录：" +  gatewayId);
		}
		gateway.connect();
		return true;
	}
	
	/**
	 * 断开网关
	 * @return
	 */
	@Override
	public boolean disconnect(String gatewayId) {
		log.info("断开网关[{}]", gatewayId);
		Gateway gateway = gatewayMgr.get(Identifier.of(gatewayId));
		if(Objects.isNull(gateway)) {
			throw new NoSuchElementException("没有该网关记录：" +  gatewayId);
		}
		gateway.disconnect();
		return true;
	}
	
	/**
	 * 模拟出入金
	 * @param money
	 * @return
	 */
	@Override
	public boolean simMoneyIO(String gatewayId, int money) {
		SimTradeGateway gateway = (SimTradeGateway) gatewayMgr.get(Identifier.of(gatewayId));
		if(Objects.isNull(gateway)) {
			throw new NoSuchElementException("没有该网关记录：" +  gatewayId);
		}
		gateway.moneyIO(money);
		if(money != 0) {			
			log.info("模拟账户[{}]，{}金：{}", gatewayId, money>0 ? "入": "出", Math.abs(money));
		}
		return true;
	}
	
	/**
	 * 活跃检测
	 * @param gatewayId
	 * @return
	 */
	public boolean isActive(String gatewayId) {
		try {
			MarketGateway gateway = (MarketGateway) gatewayMgr.get(Identifier.of(gatewayId));
			return gateway.isActive();
		} catch (ClassCastException e) {
			throw new IllegalStateException(gatewayId + "不是一个行情网关", e);
		}
	}
	
	/**
	 * 网关配置元信息
	 * @param gatewayType
	 * @return
	 */
	public Collection<ComponentField> getGatewaySettingsMetaInfo(ChannelType channelType) {
		return gatewayMetaProvider.getSettings(channelType);
	}
	
	/**
	 * 网关已订阅合约
	 * @param gatewayId
	 * @return
	 */
	public List<ContractSimpleInfo> getSubscribedContractList(String gatewayId){
		GatewayDescription gd = gatewayRepo.findById(gatewayId);
		if(gd == null) {
			throw new NoSuchElementException("没有找到网关：" + gatewayId);
		}
		return gd.getSubscribedContracts();
	}
	
	/**
	 * 复位重置回放网关
	 * @param gatewayId
	 * @return
	 * @throws Exception 
	 */
	@Override
	public boolean resetPlayback(String gatewayId) {
		log.info("复位 [{}] 行情回放网关", gatewayId);
		GatewayDescription gd = gatewayRepo.findById(gatewayId);
		playbackRtRepo.deleteById(gatewayId);
		decodeSettings(gd);
		doCreateGateway(gd);
		return true;
	}
	
	private GatewayDescription decodeSettings(GatewayDescription gd) {
		if(gd.getSettings() instanceof JSONObject) {
			return gd;
		}
		String decodeStr = CodecUtils.decrypt((String) gd.getSettings());
		if(!JSON.isValid(decodeStr)) {
			throw new IllegalStateException("解码字符串非法，很可能是临时文件夹" + System.getProperty("user.home") + File.separator
					+ ".northstar-salt这个盐文件与加密时的不一致导致无法解码。解决办法：手动移除旧的Gateway数据，重新录入，并确保盐文件不会丢失。");
		}
		gd.setSettings(JSON.parseObject(decodeStr));
		return gd;
	}
	
	@Override
	public void postLoad() {
		log.info("开始加载网关");
		List<GatewayDescription> result = gatewayRepo.findAll();
		// 因为网关有依赖关系，必须先初始化行情网关
		result.stream().filter(gd -> gd.getGatewayUsage() == GatewayUsage.MARKET_DATA).map(this::decodeSettings).forEach(gd -> {
			try {
				doCreateGateway(decodeSettings(gd));
			} catch(Exception e) {
				log.error("", e);
			}
		});
		result.stream().filter(gd -> gd.getGatewayUsage() == GatewayUsage.TRADE).map(this::decodeSettings).forEach(gd -> {
			try {
				doCreateGateway(decodeSettings(gd));
				if(gd.isAutoConnect())
					connect(gd.getGatewayId());
			} catch(Exception e) {
				log.error("", e);
			}
		});
		log.info("等待网关合约加载");
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			log.warn("", e);
		}
		// 先确保交易网关加载了合约信息，再连线行情网关
		result.stream()
			.filter(gd -> gd.getGatewayUsage() == GatewayUsage.MARKET_DATA)
			.filter(GatewayDescription::isAutoConnect)
			.map(GatewayDescription::getGatewayId)
			.forEach(this::connect);
		log.info("网关加载完毕");
	}

}