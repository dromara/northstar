package tech.xuanwu.northstar;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.domain.GatewayConnection;
import tech.xuanwu.northstar.gateway.api.Gateway;
import tech.xuanwu.northstar.model.GatewayAndConnectionManager;

@Slf4j
@Component
public class CtpScheduleTask {
	
	@Autowired
	private GatewayAndConnectionManager gatewayConnMgr;
	
	@Autowired
	private HolidayManager holidayMgr;

	@Scheduled(cron="0 0/1 0-1,9-14,21-23 ? * 1/5")
	public void timelyCheckConnection() {
		if(holidayMgr.isHoliday(LocalDate.now())) {
			return;
		}
		connectIfNotConnected();
		log.info("开盘时间连线巡检");
	}
	
	@Scheduled(cron="0 55 8,20 ? * 1/5")
	public void dailyCheckConnection() {
		if(holidayMgr.isHoliday(LocalDate.now())) {
			return;
		}
		connectIfNotConnected();
		log.info("日连线定时任务");
	}
	
	private void connectIfNotConnected() {
		for(GatewayConnection conn : gatewayConnMgr.getAllConnections()) {
			if(conn.isConnected() || !conn.getGwDescription().isAutoConnect()) {
				continue;
			}
			Gateway gateway = gatewayConnMgr.getGatewayByConnection(conn);
			gateway.connect();
			log.info("网关[{}]，自动连线", conn.getGwDescription().getGatewayId());
		}
	}
	
	@Scheduled(cron="0 1 15 ? * 1/5")
	public void dailySettlement() {
		if(holidayMgr.isHoliday(LocalDate.now())) {
			return;
		}
		log.info("日结算定时任务");
	}
}
