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
			// 十分钟检查一次
			Thread.sleep(60*10*1000);
		}
	}
	
}
