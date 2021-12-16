package tech.quantit.northstar.main.service;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.alibaba.fastjson.JSON;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.GatewayType;
import tech.quantit.northstar.common.constant.GatewayUsage;
import tech.quantit.northstar.common.event.InternalEventBus;
import tech.quantit.northstar.common.exception.NoSuchElementException;
import tech.quantit.northstar.common.model.CtpSettings;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.common.model.SimSettings;
import tech.quantit.northstar.domain.GatewayAndConnectionManager;
import tech.quantit.northstar.domain.GatewayConnection;
import tech.quantit.northstar.domain.MarketGatewayConnection;
import tech.quantit.northstar.domain.TraderGatewayConnection;
import tech.quantit.northstar.gateway.api.GatewayFactory;
import tech.quantit.northstar.gateway.api.Gateway;
import tech.quantit.northstar.gateway.api.MarketGateway;
import tech.quantit.northstar.gateway.api.domain.GlobalMarketRegistry;
import tech.quantit.northstar.gateway.api.domain.NormalContract;
import tech.quantit.northstar.gateway.sim.trade.SimGatewayFactory;
import tech.quantit.northstar.gateway.sim.trade.SimMarket;
import tech.quantit.northstar.gateway.sim.trade.SimTradeGateway;
import tech.quantit.northstar.main.persistence.GatewayRepository;
import tech.quantit.northstar.main.persistence.MarketDataRepository;
import tech.quantit.northstar.main.persistence.ModuleRepository;
import tech.quantit.northstar.main.persistence.po.GatewayPO;
import tech.quantit.northstar.main.utils.CodecUtils;
import tech.quantit.northstar.strategy.api.model.ModuleInfo;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.CtpGatewayFactory;
import xyz.redtorch.gateway.ctp.x64v6v5v1cpv.CtpSimGatewayFactory;

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
	
	private GatewayRepository gatewayRepo;
	
	private MarketDataRepository mdRepo;
	
	private InternalEventBus eventBus;
	
	private SimMarket simMarket;
	
	private ApplicationContext ctx;
	
	private ModuleRepository moduleRepo;
	
	private GlobalMarketRegistry registry;
	
	public GatewayService(GatewayAndConnectionManager gatewayConnMgr, GatewayRepository gatewayRepo, MarketDataRepository mdRepo,
			ModuleRepository moduleRepo, InternalEventBus eventBus, SimMarket simMarket, GlobalMarketRegistry registry) {
		this.gatewayConnMgr = gatewayConnMgr;
		this.gatewayRepo = gatewayRepo;
		this.mdRepo = mdRepo;
		this.moduleRepo = moduleRepo;
		this.eventBus = eventBus;
		this.simMarket = simMarket;
		this.registry = registry;
	}
	
	/**
	 * 创建网关
	 * @return
	 * @throws Exception 
	 */
	public boolean createGateway(GatewayDescription gatewayDescription) throws Exception {
		log.info("创建网关[{}]", gatewayDescription.getGatewayId());
		GatewayPO po = new GatewayPO();
		BeanUtils.copyProperties(gatewayDescription, po);
		if(null == gatewayDescription.getSettings()) {
			throw new IllegalArgumentException("网关设置不能为空");
		}
		po.setSettings(CodecUtils.encrypt(JSON.toJSONString(gatewayDescription.getSettings())));
		gatewayRepo.insert(po);
		if(gatewayDescription.getGatewayUsage() == GatewayUsage.MARKET_DATA) {
			mdRepo.init(gatewayDescription.getGatewayId());
		}
		
		return doCreateGateway(gatewayDescription);
	}
	
	private boolean doCreateGateway(GatewayDescription gatewayDescription) {
		Gateway gateway = null;
		GatewayConnection conn = gatewayDescription.getGatewayUsage() == GatewayUsage.MARKET_DATA
				? new MarketGatewayConnection(gatewayDescription, eventBus)
				: new TraderGatewayConnection(gatewayDescription, eventBus);
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
			gateway.connect();
		}
		if(gatewayDescription.getGatewayUsage() == GatewayUsage.MARKET_DATA) {
			registry.register((MarketGateway) gateway);
		}
		
		return true;
	}
	
	/**
	 * 更新网关
	 * @return
	 * @throws Exception 
	 */
	public boolean updateGateway(GatewayDescription gatewayDescription) throws Exception {
		log.info("更新网关[{}]", gatewayDescription.getGatewayId());
		GatewayPO po = new GatewayPO();
		BeanUtils.copyProperties(gatewayDescription, po);
		po.setSettings(CodecUtils.encrypt(JSON.toJSONString(gatewayDescription.getSettings())));
		gatewayRepo.save(po);
		
		// 先删除旧的，再重新创建新的
		return doDeleteGateway(gatewayDescription.getGatewayId()) && doCreateGateway(gatewayDescription);
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
			for(ModuleInfo info : moduleRepo.findAllModuleInfo()) {
				if(StringUtils.equals(info.getAccountGatewayId(), gatewayId)) {
					throw new IllegalStateException("仍有模组与本账户网关存在绑定关系，请先解除绑定！");
				}
			}
		}
		boolean flag = doDeleteGateway(gatewayId);
		gatewayRepo.deleteById(gatewayId);
		mdRepo.dropGatewayData(gatewayId);
		mdRepo.clearGatewayContract(gatewayId);
		return flag;
	}
	
	private boolean doDeleteGateway(String gatewayId) {
		GatewayConnection conn = gatewayConnMgr.getGatewayConnectionById(gatewayId);
		Gateway gateway = gatewayConnMgr.getGatewayByConnection(conn);
		gatewayConnMgr.removePair(conn);
		if(gateway instanceof SimTradeGateway simGateway) {
			String mdGatewayId = conn.getGwDescription().getBindedMktGatewayId();
			simMarket.removeGateway(mdGatewayId, simGateway);
		}
		return true;
	}
	
	/**
	 * 查询所有网关
	 * @return
	 * @throws Exception 
	 */
	public List<GatewayDescription> findAllGateway() throws Exception{
		return gatewayConnMgr.getAllConnections().stream()
				.map(conn -> conn.getGwDescription())
				.collect(Collectors.toList());
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
	public List<GatewayDescription> findAllTraderGateway() throws Exception{
		return gatewayConnMgr.getAllConnections().stream()
				.map(conn -> conn.getGwDescription())
				.filter(gwDescription -> gwDescription.getGatewayUsage() != GatewayUsage.MARKET_DATA)
				.collect(Collectors.toList());
	}
	
	/**
	 * 连接网关
	 * @return
	 */
	public boolean connect(String gatewayId) {
		log.info("连接网关[{}]", gatewayId);
		if(gatewayConnMgr.exist(gatewayId)) {
			gatewayConnMgr.getGatewayById(gatewayId).connect();
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
	
	public boolean isActive(String gatewayId) {
		try {
			MarketGateway gateway = (MarketGateway) gatewayConnMgr.getGatewayById(gatewayId);
			return gateway.isActive();
		} catch (ClassCastException e) {
			throw new IllegalStateException(gatewayId + "不是一个行情网关", e);
		}
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		List<GatewayPO> result = gatewayRepo.findAll();
		for(GatewayPO po : result) {
			GatewayDescription gd = new GatewayDescription();
			BeanUtils.copyProperties(po, gd);
			String decodeStr = CodecUtils.decrypt((String) po.getSettings());
			if(!JSON.isValid(decodeStr)) {
				throw new IllegalStateException("解码字符串非法，很可能是临时文件夹" + System.getProperty("user.home") + File.separator
						+ ".northstar-salt这个盐文件与加密时的不一致导致无法解码。解决办法：手动移除旧的Gateway数据，重新录入，并确保盐文件不会丢失。");
			}
			if(gd.getGatewayType() == GatewayType.CTP || gd.getGatewayType() == GatewayType.CTP_SIM) {
				CtpSettings settings = JSON.parseObject(decodeStr, CtpSettings.class);
				gd.setSettings(settings);
			}
			if(gd.getGatewayType() == GatewayType.SIM) {
				SimSettings settings = JSON.parseObject(decodeStr, SimSettings.class);
				gd.setSettings(settings);
			}
			doCreateGateway(gd);
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.ctx = applicationContext;
	}
}
