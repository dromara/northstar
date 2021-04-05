package tech.xuanwu.northstar.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.google.common.eventbus.Subscribe;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.constant.GatewayType;
import tech.xuanwu.northstar.common.constant.GatewayUsage;
import tech.xuanwu.northstar.common.event.InternalEventBus;
import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.common.exception.NoSuchElementException;
import tech.xuanwu.northstar.common.model.CtpSettings;
import tech.xuanwu.northstar.common.model.GatewayDescription;
import tech.xuanwu.northstar.domain.GatewayConnection;
import tech.xuanwu.northstar.domain.MarketGatewayConnection;
import tech.xuanwu.northstar.domain.TraderGatewayConnection;
import tech.xuanwu.northstar.engine.event.EventEngine;
import tech.xuanwu.northstar.gateway.api.Gateway;
import tech.xuanwu.northstar.gateway.api.TradeGateway;
import tech.xuanwu.northstar.persistence.GatewayRepository;
import tech.xuanwu.northstar.persistence.po.GatewayPO;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.CtpGatewayAdapter;
import xyz.redtorch.pb.CoreEnum.GatewayAdapterTypeEnum;
import xyz.redtorch.pb.CoreEnum.GatewayTypeEnum;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.GatewaySettingField;
import xyz.redtorch.pb.CoreField.GatewaySettingField.CtpApiSettingField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;

/**
 * 网关服务
 * @author KevinHuangwl
 *
 */
@Slf4j
@Service
public class GatewayService implements InitializingBean {
	
	protected ConcurrentHashMap<String, GatewayConnection> marketGatewayMap = new ConcurrentHashMap<>();
	protected ConcurrentHashMap<String, GatewayConnection> traderGatewayMap = new ConcurrentHashMap<>();
	protected ConcurrentHashMap<GatewayConnection, Gateway> gatewayMap = new ConcurrentHashMap<>();
	
	@Autowired
	protected GatewayRepository gatewayRepo;
	
	@Autowired
	protected EventEngine eventEngine;
	
	@Autowired
	protected InternalEventBus eventBus;
	
	/**
	 * 创建网关
	 * @return
	 */
	public boolean createGateway(GatewayDescription gatewayDescription) {
		log.info("创建网关[{}]", gatewayDescription.getGatewayId());
		GatewayPO po = new GatewayPO();
		BeanUtils.copyProperties(gatewayDescription, po);
		gatewayRepo.insert(po);
		
		return doCreateGateway(gatewayDescription);
	}
	
	private boolean doCreateGateway(GatewayDescription gatewayDescription) {
		Gateway gateway = null;
		GatewayConnection conn = null;
		GatewayTypeEnum gwType = null;
		if(gatewayDescription.getGatewayUsage() == GatewayUsage.MARKET_DATA) {
			gwType = GatewayTypeEnum.GTE_MarketData;
			conn = new MarketGatewayConnection(gatewayDescription, eventBus);
			marketGatewayMap.put(gatewayDescription.getGatewayId(), conn);
		} else {
			gwType = GatewayTypeEnum.GTE_Trade;
			conn = new TraderGatewayConnection(gatewayDescription, eventBus);
			traderGatewayMap.put(gatewayDescription.getGatewayId(), conn);
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
			gateway = new CtpGatewayAdapter(eventEngine, GatewaySettingField.newBuilder()
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
		
		if(gatewayDescription.isAutoConnect()) {
			conn.connect();
			gateway.connect();
		}
		
		gatewayMap.put(conn, gateway);
		log.info("创建成功");
		return true;
	}
	
	/**
	 * 更新网关
	 * @return
	 */
	public boolean updateGateway(GatewayDescription gatewayDescription) {
		log.info("更新网关[{}]", gatewayDescription.getGatewayId());
		GatewayPO po = new GatewayPO();
		BeanUtils.copyProperties(gatewayDescription, po);
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
		gatewayRepo.deleteById(gatewayId);
		return doDeleteGateway(gatewayId);
	}
	
	private boolean doDeleteGateway(String gatewayId) {
		GatewayConnection conn = null;
		if(marketGatewayMap.containsKey(gatewayId)) {
			conn = marketGatewayMap.remove(gatewayId);
		} else if (traderGatewayMap.containsKey(gatewayId)) {
			conn = traderGatewayMap.remove(gatewayId);
		} else {
			throw new NoSuchElementException("没有该网关记录：" +  gatewayId);
		}
		conn.disconnect();
		gatewayMap.remove(conn).disconnect();
		log.info("移除成功");
		return true;
	}
	
	/**
	 * 查询所有网关
	 * @return
	 */
	public List<GatewayDescription> findAllGateway(){
		List<GatewayDescription> resultList = new ArrayList<>(marketGatewayMap.size() + traderGatewayMap.size());
		marketGatewayMap.forEach((k, v) -> resultList.add(v.getGwDescription()));
		traderGatewayMap.forEach((k, v) -> resultList.add(v.getGwDescription()));
		return resultList;
	}
	
	/**
	 * 查询所有行情网关
	 * @return
	 */
	public List<GatewayDescription> findAllMarketGateway(){
		List<GatewayDescription> resultList = new ArrayList<>(marketGatewayMap.size());
		marketGatewayMap.forEach((k, v) -> resultList.add(v.getGwDescription()));
		return resultList;
	}
	
	/**
	 * 查询所有交易网关
	 * @return
	 */
	public List<GatewayDescription> findAllTraderGateway(){
		List<GatewayDescription> resultList = new ArrayList<>(traderGatewayMap.size());
		traderGatewayMap.forEach((k, v) -> resultList.add(v.getGwDescription()));
		return resultList;
	}
	
	/**
	 * 连接网关
	 * @return
	 */
	public boolean connect(String gatewayId) {
		log.info("连接网关[{}]", gatewayId);
		if(traderGatewayMap.containsKey(gatewayId)) {
			traderGatewayMap.get(gatewayId).connect();
		} else if (marketGatewayMap.containsKey(gatewayId)) {
			marketGatewayMap.get(gatewayId).connect();
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
		if(traderGatewayMap.containsKey(gatewayId)) {
			traderGatewayMap.get(gatewayId).disconnect();
		} else if (marketGatewayMap.containsKey(gatewayId)) {
			marketGatewayMap.get(gatewayId).disconnect();
		} else {
			throw new NoSuchElementException("没有该网关记录：" +  gatewayId);
		}
		
		return true;
	}
	
	@Subscribe
	private void onEvent(NorthstarEvent e) {
		if (e.getEvent() == NorthstarEventType.PLACE_ORDER) {
			SubmitOrderReqField submitReq = (SubmitOrderReqField) e.getData();
			String gatewayId = submitReq.getGatewayId();
			if(!traderGatewayMap.containsKey(gatewayId)) {
				throw new NoSuchElementException("没有找到相关的网关：" + gatewayId);
			}
			TradeGateway tradeGateway = (TradeGateway) traderGatewayMap.get(gatewayId);
			tradeGateway.submitOrder(submitReq);
		} else if (e.getEvent() == NorthstarEventType.WITHDRAW_ORDER) {
			CancelOrderReqField cancelReq = (CancelOrderReqField) e.getData();
			String gatewayId = cancelReq.getGatewayId();
			if(!traderGatewayMap.containsKey(gatewayId)) {
				throw new NoSuchElementException("没有找到相关的网关：" + gatewayId);
			}
			TradeGateway tradeGateway = (TradeGateway) traderGatewayMap.get(gatewayId);
			tradeGateway.cancelOrder(cancelReq);
		}
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		List<GatewayPO> result = gatewayRepo.findAll();
		for(GatewayPO po : result) {
			GatewayDescription gd = new GatewayDescription();
			BeanUtils.copyProperties(po, gd);
			doCreateGateway(gd);
		}
	}
}
