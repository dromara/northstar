package tech.xuanwu.northstar.handler;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.domain.TradeDayAccount;
import tech.xuanwu.northstar.factories.TradeDayAccountFactory;

@Slf4j
public class AccountEventHandler extends AbstractEventHandler implements InternalEventHandler{

	private Map<String, TradeDayAccount> accountMap;
	private TradeDayAccountFactory factory;
	
	public AccountEventHandler(Map<String, TradeDayAccount> accountMap, TradeDayAccountFactory factory) {
		this.accountMap = accountMap;
		this.factory = factory;
	}
	
	@Override
	public void doHandle(NorthstarEvent e) {
		if(e.getEvent() == NorthstarEventType.LOGGED_IN) {
			onLogined(e);
		} else if (e.getEvent() == NorthstarEventType.LOGGED_OUT) {
			onLogouted(e);
		}
	}

	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return eventType == NorthstarEventType.LOGGED_IN || eventType == NorthstarEventType.LOGGING_IN 
				|| eventType == NorthstarEventType.LOGGED_OUT || eventType == NorthstarEventType.LOGGING_OUT;
	}
	
	private void onLogined(NorthstarEvent e) {
		String gatewayId = (String) e.getData();
		accountMap.put(gatewayId, factory.newInstance(gatewayId));
		log.info("账户登陆：{}", gatewayId);
	}
	
	private void onLogouted(NorthstarEvent e) {
		String gatewayId = (String) e.getData();
		accountMap.remove(gatewayId);
		log.info("账户登出：{}", gatewayId);
	}

}
