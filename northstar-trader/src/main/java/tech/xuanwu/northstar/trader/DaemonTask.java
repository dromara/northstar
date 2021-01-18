package tech.xuanwu.northstar.trader;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Resource;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import tech.xuanwu.northstar.gateway.GatewayApi;
import tech.xuanwu.northstar.trader.constants.Constants;

/**
 * 守护线程
 * @author kevin
 *
 */
@Component
public class DaemonTask implements CommandLineRunner{

	@Resource(name=Constants.TRADABLE_ACCOUNT)
	ConcurrentHashMap<String, GatewayApi> tradableAccounts;
	
	@Resource(name=Constants.CTP_MARKETDATA)
	GatewayApi ctpGateway;
	
	@Override
	public void run(String... args) throws Exception {
		for(;;) {
			if(!ctpGateway.isConnected()) {
				ctpGateway.connect();
			}
			for(Entry<String, GatewayApi> e : tradableAccounts.entrySet()) {
				GatewayApi gateway = e.getValue();
				if(!gateway.isConnected()) {
					gateway.connect();
				}
			}
			// 开盘时间一分钟检查一次，非开盘时间十分钟检查一次
			Thread.sleep(isTradeTime() ? 60*1000 : 60*10*1000);
		}
	}
	
	private boolean isTradeTime() {
		int hour = LocalTime.now().getHour();
		int day = LocalDate.now().getDayOfWeek().getValue();
		boolean dayTradeTime = hour >= 9 && hour <=16;
		boolean nightTradeTime = hour >= 21 || hour < 3;
		if(day < 7 && (dayTradeTime || nightTradeTime)) {
			return true;
		}
		return false;
	}
}
