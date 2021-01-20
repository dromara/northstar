package tech.xuanwu.northstar.trader;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Resource;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.gateway.GatewayApi;
import tech.xuanwu.northstar.trader.constants.Constants;

@Slf4j
@Component
public class ScheduledTask {

	@Resource(name=Constants.TRADABLE_ACCOUNT)
	ConcurrentHashMap<String, GatewayApi> tradableAccounts;
	
	@Resource(name=Constants.CTP_MARKETDATA)
	GatewayApi ctpGateway;
	
	/**
	 * 定时连线
	 */
	@Scheduled(cron = "0 49/5 8,20 ? * MON-FRI")
	public void execute() {
		log.info("执行定时连线任务");
		if(!ctpGateway.isConnected()) {
			ctpGateway.connect();
		}
		for(Entry<String, GatewayApi> e : tradableAccounts.entrySet()) {
			GatewayApi gateway = e.getValue();
			if(!gateway.isConnected()) {
				gateway.connect();
			}
		}
	}
	
}
