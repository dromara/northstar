package tech.xuanwu.northstar.strategy;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
public class NorthstartStrategyApplication {
	
	static ScheduledExecutorService execService = Executors.newScheduledThreadPool(1);
	
	static final int HEARTBEAT_INTERVAL = 5;

	public static void main(String[] args) {
		TimeUnit unit = TimeUnit.MINUTES;
		
		SpringApplication.run(NorthstartStrategyApplication.class, args);

		execService.scheduleAtFixedRate(()->{
			
			log.info("策略进程心跳回报。回报时间间隔：{} {}", HEARTBEAT_INTERVAL, unit);
			
		}, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, unit);
	}

}
