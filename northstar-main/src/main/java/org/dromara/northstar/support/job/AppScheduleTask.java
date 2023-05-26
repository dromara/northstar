package org.dromara.northstar.support.job;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalTime;
import java.util.List;

import javax.transaction.Transactional;

import org.dromara.northstar.data.jdbc.MarketDataRepository;
import org.dromara.northstar.gateway.IMarketCenter;
import org.dromara.northstar.support.notification.IMessageSenderManager;
import org.dromara.northstar.support.utils.ExceptionLogChecker;
import org.dromara.northstar.support.utils.InetAddressUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import lombok.extern.slf4j.Slf4j;

/**
 * 主程序定时检查任务
 * @author KevinHuangwl
 *
 */
@Slf4j
@Component
public class AppScheduleTask {
	
	@Autowired
	private IMarketCenter mktCenter;
	
	@Autowired
	private MarketDataRepository mdRepo;
	
	@Autowired
	private IMessageSenderManager msgMgr;

	/**
	 * K线数据合成检查
	 */
	@Scheduled(cron="0 0/1 * ? * 1-6")
	public void sectionFinishUp() {
		mktCenter.endOfMarketTime();
		log.debug("K线数据合成检查");
	}
	
	/**
	 * 检查当天的程序日志中是否存在异常日志，如存在则转发报告
	 * 周一至五，每隔一小时检查 
	 * @throws IOException 
	 */
	@Scheduled(cron="0 0 0/1 ? * 1-5")
	public void checkAppException() throws IOException {
		log.debug("检查当天的程序日志中是否存在异常日志");
		Logger logger = (Logger) log;
		FileAppender<ILoggingEvent> fileAppender = (FileAppender<ILoggingEvent>) logger.getLoggerContext().getLogger("ROOT").getAppender("FILE");
		File logFile = new File(fileAppender.getFile());
		FileReader fr = new FileReader(logFile);
		ExceptionLogChecker checker = new ExceptionLogChecker(fr);
		LocalTime endTime = LocalTime.now();
		LocalTime startTime = endTime.minusHours(1);
		List<String> errorLines = checker.getExceptionLog(startTime, endTime);
		if(!errorLines.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			errorLines.forEach(line -> sb.append(line + "%n"));
			sb.append(String.format("%n%n警报来源：%s", InetAddressUtils.getHostname()));
			msgMgr.getSubscribers().forEach(sub -> 
				msgMgr.getSender().send(sub, String.format("[程序异常日志警报] %d-%d时，%d条异常记录", 
						startTime.getHour(), endTime.getHour(), errorLines.size()), sb.toString())
			);
		}
	}
	
	/**
	 * 移除过期行情数据
	 */
	@Scheduled(cron="0 30 20 ? * 1-5")
	@Transactional
	public void removeExpiredData() {
		mdRepo.deleteByExpiredAtBefore(System.currentTimeMillis());
		log.debug("移除过期行情数据");
	}
}
