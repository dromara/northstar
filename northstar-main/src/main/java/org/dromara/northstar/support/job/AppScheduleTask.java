package org.dromara.northstar.support.job;

import java.time.LocalDateTime;

import org.dromara.northstar.account.GatewayManager;
import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.gateway.Gateway;
import org.dromara.northstar.gateway.IMarketCenter;
import org.dromara.northstar.support.holiday.CtpHolidayManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.gateway.ctp.common.GatewayConstants;

@Slf4j
@Component
public class AppScheduleTask {
	
	@Autowired
	private GatewayManager gatewayMgr;
	
	@Autowired
	private CtpHolidayManager holidayMgr;
	
	@Autowired
	private IMarketCenter mktCenter;

	@Scheduled(cron="0 0/1 0-1,9-14,21-23 ? * 1-5")
	public void timelyCheckConnection() {
		if(holidayMgr.isHoliday(LocalDateTime.now())) {
			return;
		}
		GatewayConstants.SMART_CONNECTOR.update();
		connectIfNotConnected();
		log.debug("开盘时间连线巡检");
	}
	
	@Scheduled(cron="0 55 8,20 ? * 1-5")
	public void dailyCheckConnection() throws InterruptedException {
		if(holidayMgr.isHoliday(LocalDateTime.now())) {
			log.debug("当前为假期，不进行连线");
			return;
		}
		GatewayConstants.SMART_CONNECTOR.update();
		Thread.sleep(10000);
		connectIfNotConnected();
		log.info("日连线定时任务");
	}
	
	private void connectIfNotConnected() {
		gatewayMgr.allGateways().stream()
			.filter(gw -> gw.gatewayDescription().isAutoConnect())
			.filter(gw -> gw.getConnectionState() != ConnectionState.CONNECTED)
			.forEach(Gateway::connect);
	}
	
	/**
	 * K线数据合成检查
	 */
	@Scheduled(cron="10 0/1 * ? * 1-6")
	public void sectionFinishUp() {
		if(holidayMgr.isHoliday(LocalDateTime.now())) {
			return;
		}
		mktCenter.endOfMarketTime();
		log.debug("K线数据合成检查");
	}
	
	@Scheduled(cron="0 1 15 ? * 1-5")
	public void dailySettlement() {
		if(holidayMgr.isHoliday(LocalDateTime.now())) {
			log.debug("当前为假期，没有结算任务");
			return;
		}
		log.info("日结算定时任务");
	}

}
