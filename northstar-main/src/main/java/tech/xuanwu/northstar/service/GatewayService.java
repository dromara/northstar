package tech.xuanwu.northstar.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;

import com.alibaba.fastjson.JSON;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.constant.GatewayType;
import tech.xuanwu.northstar.common.constant.GatewayUsage;
import tech.xuanwu.northstar.common.event.InternalEventBus;
import tech.xuanwu.northstar.common.exception.NoSuchElementException;
import tech.xuanwu.northstar.common.model.CtpSettings;
import tech.xuanwu.northstar.common.model.GatewayDescription;
import tech.xuanwu.northstar.domain.GatewayConnection;
import tech.xuanwu.northstar.domain.MarketGatewayConnection;
import tech.xuanwu.northstar.domain.TraderGatewayConnection;
import tech.xuanwu.northstar.engine.event.FastEventEngine;
import tech.xuanwu.northstar.gateway.api.Gateway;
import tech.xuanwu.northstar.model.GatewayAndConnectionManager;
import tech.xuanwu.northstar.persistence.GatewayRepository;
import tech.xuanwu.northstar.persistence.MarketDataRepository;
import tech.xuanwu.northstar.persistence.po.GatewayPO;
import tech.xuanwu.northstar.utils.CodecUtils;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.CtpGatewayAdapter;
import xyz.redtorch.pb.CoreEnum.GatewayAdapterTypeEnum;
import xyz.redtorch.pb.CoreEnum.GatewayTypeEnum;
import xyz.redtorch.pb.CoreField.GatewaySettingField;
import xyz.redtorch.pb.CoreField.GatewaySettingField.CtpApiSettingField;

/**
 * 网关服务
 * @author KevinHuangwl
 *
 */
@Slf4j
public class GatewayService implements InitializingBean {
	
	private GatewayAndConnectionManager gatewayConnMgr;
	
	private GatewayRepository gatewayRepo;
	
	private MarketDataRepository mdRepo;
	
	private FastEventEngine fastEventEngine;
	
	private InternalEventBus eventBus;
	
	public GatewayService(GatewayAndConnectionManager gatewayConnMgr, GatewayRepository gatewayRepo, MarketDataRepository mdRepo,
			FastEventEngine fastEventEngine, InternalEventBus eventBus) {
		this.gatewayConnMgr = gatewayConnMgr;
		this.gatewayRepo = gatewayRepo;
		this.mdRepo = mdRepo;
		this.fastEventEngine = fastEventEngine;
		this.eventBus = eventBus;
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
		po.setSettings(CodecUtils.encrypt(JSON.toJSONString(gatewayDescription.getSettings())));
		gatewayRepo.insert(po);
		if(gatewayDescription.getGatewayUsage() == GatewayUsage.MARKET_DATA) {
			mdRepo.init(gatewayDescription.getGatewayId());
		}
		
		return doCreateGateway(gatewayDescription);
	}
	
	private boolean doCreateGateway(GatewayDescription gatewayDescription) {
		Gateway gateway = null;
		GatewayConnection conn = null;
		GatewayTypeEnum gwType = null;
		if(gatewayDescription.getGatewayUsage() == GatewayUsage.MARKET_DATA) {
			gwType = GatewayTypeEnum.GTE_MarketData;
			conn = new MarketGatewayConnection(gatewayDescription, eventBus);
		} else {
			gwType = GatewayTypeEnum.GTE_Trade;
			conn = new TraderGatewayConnection(gatewayDescription, eventBus);
		}
		
		if(gatewayDescription.getGatewayType() == GatewayType.CTP) {
			CtpSettings settings = JSON.toJavaObject((JSON)JSON.toJSON(gatewayDescription.getSettings()), CtpSettings.class);
			CtpApiSettingField ctpSetting = CtpApiSettingField.newBuilder()
					.setAppId(settings.getAppId())
					.setAuthCode(settings.getAuthCode())
					.setBrokerId(settings.getBrokerId())
					.setMdHost(settings.getMdHost())
					.setMdPort(settings.getMdPort())
					.setTdHost(settings.getTdHost())
					.setTdPort(settings.getTdPort())
					.setPassword(settings.getPassword())
					.setUserId(settings.getUserId())
					.setUserProductInfo(settings.getUserProductInfo())
					.build();
			gateway = new CtpGatewayAdapter(fastEventEngine, GatewaySettingField.newBuilder()
					.setGatewayAdapterType(GatewayAdapterTypeEnum.GAT_CTP)
					.setGatewayId(gatewayDescription.getGatewayId())
					.setGatewayName(gatewayDescription.getGatewayId())
					.setCtpApiSetting(ctpSetting)
					.setGatewayType(gwType)
					.build());
		} else if(gatewayDescription.getGatewayType() == GatewayType.IB) {
			// TODO IB网关
		} else {
			throw new NoSuchElementException("没有这种网关类型：" + gatewayDescription.getGatewayType());
		}
		
		gatewayConnMgr.createPair(conn, gateway);
		if(gatewayDescription.isAutoConnect()) {
			gateway.connect();
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
		boolean flag = doDeleteGateway(gatewayId);
		gatewayRepo.deleteById(gatewayId);
		return flag;
	}
	
	private boolean doDeleteGateway(String gatewayId) {
		GatewayConnection conn = null;
		if(gatewayConnMgr.exist(gatewayId)) {
			conn = gatewayConnMgr.getGatewayConnectionById(gatewayId);
		} else {
			throw new NoSuchElementException("没有该网关记录：" +  gatewayId);
		}
		if(conn.isConnected()) {
			throw new IllegalStateException("非断开状态的网关不能删除");
		}
		gatewayConnMgr.removePair(conn);
		return true;
	}
	
	/**
	 * 查询所有网关
	 * @return
	 * @throws Exception 
	 */
	public List<GatewayDescription> findAllGateway() throws Exception{
		List<GatewayConnection> listConn = gatewayConnMgr.getAllConnections();
		for(GatewayConnection conn : listConn) {			
			conn.getGwDescription().setSettings(CodecUtils.decrypt((String) conn.getGwDescription().getSettings()));
		}
		return listConn.stream()
				.map(conn -> conn.getGwDescription())
				.collect(Collectors.toList());
	}
	
	/**
	 * 查询所有行情网关
	 * @return
	 * @throws Exception 
	 */
	public List<GatewayDescription> findAllMarketGateway() throws Exception{
		List<GatewayConnection> listConn = gatewayConnMgr.getAllConnections();
		for(GatewayConnection conn : listConn) {			
			conn.getGwDescription().setSettings(CodecUtils.decrypt((String) conn.getGwDescription().getSettings()));
		}
		return listConn.stream()
				.map(conn -> conn.getGwDescription())
				.filter(gwDescription -> gwDescription.getGatewayUsage() == GatewayUsage.MARKET_DATA)
				.collect(Collectors.toList());
	}
	
	/**
	 * 查询所有交易网关
	 * @return
	 * @throws Exception 
	 */
	public List<GatewayDescription> findAllTraderGateway() throws Exception{
		List<GatewayConnection> listConn = gatewayConnMgr.getAllConnections();
		for(GatewayConnection conn : listConn) {			
			conn.getGwDescription().setSettings(CodecUtils.decrypt((String) conn.getGwDescription().getSettings()));
		}
		return listConn.stream()
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
	
	@Override
	public void afterPropertiesSet() throws Exception {
		List<GatewayPO> result = gatewayRepo.findAll();
		for(GatewayPO po : result) {
			GatewayDescription gd = new GatewayDescription();
			BeanUtils.copyProperties(po, gd);
			gd.setSettings(CodecUtils.decrypt((String) po.getSettings()));
			doCreateGateway(gd);
		}
	}
}
