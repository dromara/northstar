package org.dromara.northstar.gateway.ctp;

import java.time.LocalDateTime;

import org.dromara.northstar.common.ObjectManager;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.gateway.Gateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.gateway.ctp.common.GatewayConstants;

@Slf4j
@Component
public class CtpDailyScheduleTask {

	@Autowired
	private ObjectManager<Gateway> gatewayMgr;
	
	@Autowired
	private CtpHolidayManager holidayMgr;
	
	/**
	 * 网关开盘前连线检查
	 * @throws InterruptedException
	 */
	@Scheduled(cron="0 55 8,20 ? * 1-5")
	public void dailyConnection() throws InterruptedException {
		if(holidayMgr.isHoliday(LocalDateTime.now())) {
			log.debug("当前为假期，不执行连线任务");
			return;
		}
		GatewayConstants.SMART_CONNECTOR.update();
		Thread.sleep(10000);
		connectIfNotConnected();
		log.info("定时连线任务");
	}
	/**
	 * 网关定时断开
	 * @throws InterruptedException
	 */
	@Scheduled(cron="0 30 2,15 ? * 1-6")
	public void dailyDisconnection() throws InterruptedException {
		if(holidayMgr.isHoliday(LocalDateTime.now())) {
			log.debug("当前为假期，不执行离线任务");
			return;
		}
		gatewayMgr.findAll().stream()
			.filter(gw -> gw.gatewayDescription().getChannelType() == ChannelType.CTP)
			.filter(gw -> gw.getConnectionState() == ConnectionState.CONNECTED)
			.forEach(Gateway::disconnect);
		log.info("定时离线任务");
	}
	/**
	 * 网关响应速度检查
	 */
	@Scheduled(cron="0 0/1 0-1,9-14,21-23 ? * 1-5")
	public void timelyCheckConnection() {
		if(holidayMgr.isHoliday(LocalDateTime.now())) {
			return;
		}
		GatewayConstants.SMART_CONNECTOR.update();
		connectIfNotConnected();
		log.debug("开盘时间连线巡检");
	}
	
	private void connectIfNotConnected() {
		gatewayMgr.findAll().stream()
			.filter(gw -> gw.gatewayDescription().getChannelType() == ChannelType.CTP)
			.filter(gw -> gw.gatewayDescription().isAutoConnect())
			.filter(gw -> gw.getConnectionState() != ConnectionState.CONNECTED)
			.forEach(Gateway::connect);
	}
}
