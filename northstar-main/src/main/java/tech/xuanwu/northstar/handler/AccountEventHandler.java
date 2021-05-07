package tech.xuanwu.northstar.handler;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.domain.TradeDayAccount;
import tech.xuanwu.northstar.factories.TradeDayAccountFactory;

@Slf4j
public class AccountEventHandler extends AbstractEventHandler implements InternalEventHandler{

	private Map<String, TradeDayAccount> accountMap;
	private TradeDayAccountFactory factory;
	
	private final Set<NorthstarEventType> TARGET_TYPE = new HashSet<>() {
		private static final long serialVersionUID = 6418831877479036414L;
		{
			this.add(NorthstarEventType.LOGGED_IN);
			this.add(NorthstarEventType.LOGGING_IN);
			this.add(NorthstarEventType.LOGGED_OUT);
			this.add(NorthstarEventType.LOGGING_OUT);
		}
	};
	
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
		return TARGET_TYPE.contains(eventType);
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
