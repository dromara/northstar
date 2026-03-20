package org.dromara.northstar.support.job;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

import org.dromara.northstar.data.jdbc.MarketDataRepository;
import org.dromara.northstar.strategy.IMessageSender;
import org.dromara.northstar.support.msg.MessageSenderManager;
import org.dromara.northstar.support.utils.ExceptionLogChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import jakarta.transaction.Transactional;
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
	private MarketDataRepository mdRepo;
	
	@Autowired
	private MessageSenderManager senderMgr;
	
	@Scheduled(cron="0 0/5 * * * *")
	public void doCheck() {
		Runtime runtime = Runtime.getRuntime();
		long totalMemory = runtime.totalMemory(); // JVM分配的总内存
		long freeMemory = runtime.freeMemory(); // JVM中未使用的内存
		long maxMemory = runtime.maxMemory(); // JVM可以使用的最大内存
		long availableMemory = maxMemory - (totalMemory - freeMemory); // 可用内存
		long totalMemoryInMegaByte = totalMemory / 1024 / 1024;
		long maxMemoryInMegaByte = maxMemory / 1024 / 1024;
		long availableMemoryInMegaByte = availableMemory / 1024 /1024;
		log.info("程序心跳检测：总内存 {}M，最大内存 {}M，可用内存 {}M", totalMemoryInMegaByte, maxMemoryInMegaByte, availableMemoryInMegaByte);
	}
	
	/**
	 * 检查当天的程序日志中是否存在异常日志，如存在则转发报告
	 * 周一至五，每隔一小时检查 
	 * @throws IOException 
	 */
	@Scheduled(cron="0 0 0/1 ? * 1-5")
	public void checkAppException() throws IOException {
		List<IMessageSender> senders = senderMgr.findAll();
		if(Objects.isNull(senders.isEmpty())) {
			log.warn("没有提供消息告警发送器，告警信息将无法发送");
			return;
		}
		for(IMessageSender msgSender : senders) {
			log.debug("检查当天的程序日志中是否存在异常日志");
			Logger logger = (Logger) log;
			FileAppender<ILoggingEvent> fileAppender = (FileAppender<ILoggingEvent>) logger.getLoggerContext().getLogger("org.dromara.northstar").getAppender("FILE");
			File logFile = new File(fileAppender.getFile());
			FileReader fr = new FileReader(logFile);
			ExceptionLogChecker checker = new ExceptionLogChecker(fr);
			LocalTime endTime = LocalTime.now();
			LocalTime startTime = endTime.minusHours(1);
			List<String> errorLines = checker.getExceptionLog(startTime, endTime);
			if(!errorLines.isEmpty()) {
				StringBuilder sb = new StringBuilder();
				errorLines.forEach(line -> sb.append(line + "\n"));
				msgSender.send(String.format("[程序异常日志警报] %d-%d时，%d条异常记录", 
						startTime.getHour(), endTime.getHour(), errorLines.size()), sb.toString());
			}
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
