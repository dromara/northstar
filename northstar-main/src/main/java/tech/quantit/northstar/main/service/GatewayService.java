package tech.quantit.northstar.main.service;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.dao.DuplicateKeyException;

import com.alibaba.fastjson.JSON;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.GatewayType;
import tech.quantit.northstar.common.constant.GatewayUsage;
import tech.quantit.northstar.common.exception.NoSuchElementException;
import tech.quantit.northstar.common.model.ContractDefinition;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.common.model.ModuleAccountDescription;
import tech.quantit.northstar.common.model.ModuleDescription;
import tech.quantit.northstar.data.IGatewayRepository;
import tech.quantit.northstar.data.IMarketDataRepository;
import tech.quantit.northstar.data.IModuleRepository;
import tech.quantit.northstar.data.ISimAccountRepository;
import tech.quantit.northstar.domain.gateway.ContractManager;
import tech.quantit.northstar.domain.gateway.GatewayAndConnectionManager;
import tech.quantit.northstar.domain.gateway.GatewayConnection;
import tech.quantit.northstar.gateway.api.Gateway;
import tech.quantit.northstar.gateway.api.GatewayFactory;
import tech.quantit.northstar.gateway.api.MarketGateway;
import tech.quantit.northstar.gateway.sim.trade.SimGatewayFactory;
import tech.quantit.northstar.gateway.sim.trade.SimTradeGateway;
import tech.quantit.northstar.main.utils.CodecUtils;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.CtpGatewayFactory;
import xyz.redtorch.gateway.ctp.x64v6v5v1cpv.CtpSimGatewayFactory;
import xyz.redtorch.pb.CoreField.ContractField;

/**
 * 网关服务
 * 注意：GatewaySetting为了防止数据库被攻破，因此对其做了对称加密，并且会在部署的机器上写入一段随机密码。
 * 只有在代码、数据库、服务器随机密码同时被攻破时，才有可能对GatewaySetting信息进行解码
 * @author KevinHuangwl
 *
 */
@Slf4j
public class GatewayService implements InitializingBean, ApplicationContextAware{
	
	private GatewayAndConnectionManager gatewayConnMgr;
	
	private IGatewayRepository gatewayRepo;
	
	private IMarketDataRepository mdRepo;
	
	private ISimAccountRepository simAccRepo;
	
	private ApplicationContext ctx;
	
	private IModuleRepository moduleRepo;
	
	private ContractManager contractMgr;
	
	public GatewayService(GatewayAndConnectionManager gatewayConnMgr, IGatewayRepository gatewayRepo, IMarketDataRepository mdRepo,
			IModuleRepository moduleRepo, ISimAccountRepository simAccRepo, ContractManager contractMgr) {
		this.gatewayConnMgr = gatewayConnMgr;
		this.gatewayRepo = gatewayRepo;
		this.mdRepo = mdRepo;
		this.contractMgr = contractMgr;
		this.moduleRepo = moduleRepo;
		this.simAccRepo = simAccRepo;
	}
	
	/**
	 * 创建网关
	 * @return
	 * @throws Exception 
	 */
	public boolean createGateway(GatewayDescription gatewayDescription) throws Exception {
		log.info("创建网关[{}]", gatewayDescription.getGatewayId());
		doSaveGatewayDescription(gatewayDescription);
		
		return doCreateGateway(gatewayDescription);
	}
	
	private boolean doCreateGateway(GatewayDescription gatewayDescription) {
		Gateway gateway = null;
		GatewayConnection conn = new GatewayConnection(gatewayDescription);
		GatewayFactory factory = null;
		if(gatewayDescription.getGatewayType() == GatewayType.CTP) {
			factory = ctx.getBean(CtpGatewayFactory.class);
		} else if(gatewayDescription.getGatewayType() == GatewayType.SIM) {
			factory = ctx.getBean(SimGatewayFactory.class);
		} else if(gatewayDescription.getGatewayType() == GatewayType.CTP_SIM) {
			factory = ctx.getBean(CtpSimGatewayFactory.class);
		} else if(gatewayDescription.getGatewayType() == GatewayType.IB) {
			// TODO IB网关
		} else {
			throw new NoSuchElementException("没有这种网关类型：" + gatewayDescription.getGatewayType());
		}
		gateway = factory.newInstance(gatewayDescription);
		gatewayConnMgr.createPair(conn, gateway);
		if(gatewayDescription.isAutoConnect()) {
			connect(gatewayDescription.getGatewayId());
		}
		
		return true;
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
	public boolean updateGateway(GatewayDescription gatewayDescription) throws Exception {
		log.info("更新网关[{}]", gatewayDescription.getGatewayId());
		doDeleteGateway(gatewayDescription.getGatewayId());
		doSaveGatewayDescription(gatewayDescription);
		// 先删除旧的，再重新创建新的
		return doCreateGateway(gatewayDescription);
	}
	
	/**
	 * 移除网关
	 * @return
	 */
	public boolean deleteGateway(String gatewayId) {
		log.info("移除网关[{}]", gatewayId);
		GatewayConnection conn = null;
		if(gatewayConnMgr.exist(gatewayId)) {
			conn = gatewayConnMgr.getGatewayConnectionById(gatewayId);
		} else {
			throw new NoSuchElementException("没有该网关记录：" +  gatewayId);
		}
		if(conn.isConnected()) {
			throw new IllegalStateException("非断开状态的网关不能删除");
		}
		if(conn.getGwDescription().getGatewayUsage() == GatewayUsage.MARKET_DATA) {			
			for(GatewayConnection gc : gatewayConnMgr.getAllConnections()) {
				if(StringUtils.equals(gc.getGwDescription().getBindedMktGatewayId(), gatewayId)) {
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
		mdRepo.dropGatewayData(gatewayId);
		simAccRepo.deleteById(gatewayId);
		return flag;
	}
	
	private boolean doDeleteGateway(String gatewayId) {
		GatewayConnection conn = gatewayConnMgr.getGatewayConnectionById(gatewayId);
		Gateway gateway = gatewayConnMgr.getGatewayByConnection(conn);
		gatewayConnMgr.removePair(conn);
		gatewayRepo.deleteById(gatewayId);
		if(gateway instanceof SimTradeGateway simGateway) {
			simGateway.destory();
		}
		return true;
	}
	
	/**
	 * 查询所有网关
	 * @return
	 * @throws Exception 
	 */
	public List<GatewayDescription> findAllGateway() {
		return gatewayConnMgr.getAllConnections().stream()
				.map(GatewayConnection::getGwDescription)
				.toList();
	}
	
	/**
	 * 查询所有行情网关
	 * @return
	 * @throws Exception 
	 */
	public List<GatewayDescription> findAllMarketGateway() {
		return gatewayConnMgr.getAllConnections().stream()
				.map(GatewayConnection::getGwDescription)
				.filter(gwDescription -> gwDescription.getGatewayUsage() == GatewayUsage.MARKET_DATA)
				.toList();
	}
	
	/**
	 * 查询所有交易网关
	 * @return
	 * @throws Exception 
	 */
	public List<GatewayDescription> findAllTraderGateway() {
		return gatewayConnMgr.getAllConnections().stream()
				.map(GatewayConnection::getGwDescription)
				.filter(gwDescription -> gwDescription.getGatewayUsage() != GatewayUsage.MARKET_DATA)
				.toList();
	}
	
	/**
	 * 连接网关
	 * @return
	 */
	public boolean connect(String gatewayId) {
		log.info("连接网关[{}]", gatewayId);
		if(gatewayConnMgr.exist(gatewayId)) {
			Gateway gateway = gatewayConnMgr.getGatewayById(gatewayId);
			gateway.connect();
		} else {
			throw new NoSuchElementException("没有该网关记录：" +  gatewayId);
		}
		
		return true;
	}
	
	/**
	 * 断开网关
	 * @return
	 */
	public boolean disconnect(String gatewayId) {
		log.info("断开网关[{}]", gatewayId);
		if(gatewayConnMgr.exist(gatewayId)) {
			gatewayConnMgr.getGatewayById(gatewayId).disconnect();
		} else {
			throw new NoSuchElementException("没有该网关记录：" +  gatewayId);
		}
		
		return true;
	}
	
	/**
	 * 模拟出入金
	 * @param money
	 * @return
	 */
	public boolean simMoneyIO(String gatewayId, int money) {
		SimTradeGateway gateway = (SimTradeGateway) gatewayConnMgr.getGatewayById(gatewayId);
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
			MarketGateway gateway = (MarketGateway) gatewayConnMgr.getGatewayById(gatewayId);
			return gateway.isActive();
		} catch (ClassCastException e) {
			throw new IllegalStateException(gatewayId + "不是一个行情网关", e);
		}
	}
	
	/**
	 * 获取合约定义
	 * @param gatewayType
	 * @return
	 */
	public List<ContractDefinition> contractDefinitions(GatewayType gatewayType){
		return contractMgr.getAllContractDefinitions().stream()
				.filter(def -> def.getGatewayType() == gatewayType)
				.toList();
	}
	
	/**
	 * 获取已订阅合约
	 * @param gatewayType
	 * @return
	 */
	public List<byte[]> getSubscribedContracts(GatewayType gatewayType){
		Map<String, GatewayDescription> resultMap = gatewayRepo.findAll().stream().collect(Collectors.toMap(GatewayDescription::getGatewayId, item -> item));
		GatewayDescription gd = resultMap.get(gatewayType.toString());
		if(gd == null) {
			throw new NoSuchElementException("找不到网关信息：" + gatewayType);
		}
		List<ContractField> resultList = new LinkedList<>();
		for(String contractDefId : gd.getSubscribedContractGroups()) {
			resultList.addAll(contractMgr.relativeContracts(contractDefId));
		}
		return resultList.stream().map(ContractField::toByteArray).toList();
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		List<GatewayDescription> result = gatewayRepo.findAll();
		for(GatewayDescription gd : result) {
			String decodeStr = CodecUtils.decrypt((String) gd.getSettings());
			if(!JSON.isValid(decodeStr)) {
				throw new IllegalStateException("解码字符串非法，很可能是临时文件夹" + System.getProperty("user.home") + File.separator
						+ ".northstar-salt这个盐文件与加密时的不一致导致无法解码。解决办法：手动移除旧的Gateway数据，重新录入，并确保盐文件不会丢失。");
			}
			gd.setSettings(JSON.parseObject(decodeStr));
			doCreateGateway(gd);
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.ctx = applicationContext;
	}
}