package tech.xuanwu.northstar;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.constant.GatewayType;
import tech.xuanwu.northstar.common.constant.GatewayUsage;
import tech.xuanwu.northstar.domain.GatewayConnection;
import tech.xuanwu.northstar.gateway.api.Gateway;
import tech.xuanwu.northstar.model.BarBufferManager;
import tech.xuanwu.northstar.model.GatewayAndConnectionManager;
import tech.xuanwu.northstar.persistence.MarketDataRepository;

@Slf4j
@Component
public class CtpScheduleTask {
	
	@Autowired
	private GatewayAndConnectionManager gatewayConnMgr;
	
	@Autowired
	private HolidayManager holidayMgr;
	
	@Autowired
	private MarketDataRepository mdRepo;
	
	@Autowired
	private BarBufferManager bbMgr;

	@Scheduled(cron="0 0/1 0-1,9-14,21-23 ? * 1-5")
	public void timelyCheckConnection() {
		if(holidayMgr.isHoliday(LocalDate.now())) {
			return;
		}
		connectIfNotConnected();
		log.debug("开盘时间连线巡检");
	}
	
	@Scheduled(cron="0 55 8,20 ? * 1-5")
	public void dailyCheckConnection() {
		if(holidayMgr.isHoliday(LocalDate.now())) {
			log.debug("当前为假期，不进行连线");
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
	
	@Scheduled(cron="58 0 9,21 ? * 1-5")
	public void dataCleaning() {
		if(holidayMgr.isHoliday(LocalDate.now())) {
			return;
		}
		LocalDateTime now = LocalDateTime.now();
		if(now.getHour() == 9) {
			// 早盘，清洗凌晨2:30至9点的数据记录,共390分钟
			LocalDateTime earlyTime = now.minus(391, ChronoUnit.MINUTES);
			ctpGatewayDataCleaning(earlyTime, now);
		} else {
			// 夜盘，清洗下午3点至晚上9点的数据记录
			LocalDateTime earlyTime = now.minus(361, ChronoUnit.MINUTES);
			ctpGatewayDataCleaning(earlyTime, now);
		}
		log.info("CTP非交易时间数据清洗任务");
	}
	
	private void ctpGatewayDataCleaning(LocalDateTime start, LocalDateTime end) {
		for(GatewayConnection conn : gatewayConnMgr.getAllConnections()) {
			if(conn.getGwDescription().getGatewayType() == GatewayType.CTP 
					|| conn.getGwDescription().getGatewayUsage() == GatewayUsage.MARKET_DATA) {
				long startTime = start.toInstant(ZoneOffset.of("+8")).toEpochMilli();
				long endTime = end.toInstant(ZoneOffset.of("+8")).toEpochMilli();
				mdRepo.clearDataByTime(conn.getGwDescription().getGatewayId(), startTime, endTime);
			}
		}
	}

	/**
	 * 开盘时间定时持久化
	 * 这里挑了每分钟的第55秒持久化，主要是考虑配置数据清洗动作。
	 * 正常来讲，开盘第一分钟的bar数据会在至少一分钟后才会生成成bar并提交到BarBufferManager，
	 * 那么，如果未满足一分钟却有数据在buffer中，那么这些数据肯定是垃圾数据
	 */
	@Scheduled(cron="55 0/1 0-2,9-14,21-23 ? * 1-5")
	public void timelySaveBar() {
		if(holidayMgr.isHoliday(LocalDate.now())) {
			return;
		}
		bbMgr.saveAndClear();
		log.info("交易时间定时持久化Bar数据任务");
	}
	
	/**
	 * 收盘时间定时持久化
	 */
	@Scheduled(cron="10 0 3,15 ? * 1-5")
	public void saveBarAfterMarketClose() {
		if(holidayMgr.isHoliday(LocalDate.now())) {
			return;
		}
		bbMgr.saveAndClear();
		log.info("收盘时间定时持久化Bar数据任务");
	}
	
	@Scheduled(cron="0 1 15 ? * 1-5")
	public void dailySettlement() {
		if(holidayMgr.isHoliday(LocalDate.now())) {
			return;
		}
		log.info("日结算定时任务");
	}

}
