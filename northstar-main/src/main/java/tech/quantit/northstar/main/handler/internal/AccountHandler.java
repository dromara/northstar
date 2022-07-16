package tech.quantit.northstar.main.handler.internal;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.event.AbstractEventHandler;
import tech.quantit.northstar.common.event.GenericEventHandler;
import tech.quantit.northstar.common.event.NorthstarEvent;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.exception.NoSuchElementException;
import tech.quantit.northstar.domain.account.TradeDayAccount;
import tech.quantit.northstar.domain.account.TradeDayAccountFactory;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 处理账户相关操作
 * @author KevinHuangwl
 *
 */
@Slf4j
public class AccountHandler extends AbstractEventHandler implements GenericEventHandler{

	private Map<String, TradeDayAccount> accountMap;
	private TradeDayAccountFactory factory;
	
	private static final Set<NorthstarEventType> TARGET_TYPE = EnumSet.of(
			NorthstarEventType.LOGGED_IN, 
			NorthstarEventType.LOGGED_OUT,
			NorthstarEventType.ACCOUNT,
			NorthstarEventType.POSITION,
			NorthstarEventType.TRADE,
			NorthstarEventType.ORDER
	); 
			
	public AccountHandler(Map<String, TradeDayAccount> accountMap, TradeDayAccountFactory factory) {
		this.accountMap = accountMap;
		this.factory = factory;
	}
	
	@Override
	public synchronized void doHandle(NorthstarEvent e) {
		switch(e.getEvent()) {
		case LOGGED_IN -> {
			String gatewayId = (String) e.getData();
			accountMap.put(gatewayId, factory.newInstance(gatewayId));
			log.info("账户登陆：{}", gatewayId);
		}
		case LOGGED_OUT -> {
			String gatewayId = (String) e.getData();
			accountMap.remove(gatewayId);
			log.info("账户登出：{}", gatewayId);
		}
		case ACCOUNT -> {
			AccountField af = (AccountField) e.getData();
			checkAccount(af.getGatewayId(), e.getEvent());
			TradeDayAccount account = accountMap.get(af.getGatewayId());
			account.onAccountUpdate(af);
		}
		case POSITION -> {
			PositionField pf = (PositionField) e.getData();
			checkAccount(pf.getGatewayId(), e.getEvent());
			TradeDayAccount account = accountMap.get(pf.getGatewayId());
			account.onPositionUpdate(pf);
		}
		case TRADE -> {
			TradeField tf = (TradeField) e.getData();
			checkAccount(tf.getGatewayId(), e.getEvent());
			TradeDayAccount account = accountMap.get(tf.getGatewayId());
			account.onTradeUpdate(tf);
		}
		case ORDER -> {
			OrderField of = (OrderField) e.getData();
			checkAccount(of.getGatewayId(), e.getEvent());
			TradeDayAccount account = accountMap.get(of.getGatewayId());
			account.onOrderUpdate(of);
		}
		default -> throw new IllegalArgumentException("Unexpected value: " + e.getEvent());
		}
	}
	
	private void checkAccount(String gatewayId, NorthstarEventType eventType) {
		if(!accountMap.containsKey(gatewayId)) {
			throw new NoSuchElementException(String.format("[%s] 找不到网关：%s", eventType, gatewayId));
		}
	}

	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return TARGET_TYPE.contains(eventType);
	}
	
}
