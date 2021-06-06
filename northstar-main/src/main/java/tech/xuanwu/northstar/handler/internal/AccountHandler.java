package tech.xuanwu.northstar.handler.internal;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.domain.account.TradeDayAccount;
import tech.xuanwu.northstar.factories.TradeDayAccountFactory;
import tech.xuanwu.northstar.handler.AbstractEventHandler;
import tech.xuanwu.northstar.handler.GenericEventHandler;
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
	
	private final Set<NorthstarEventType> TARGET_TYPE = new HashSet<>() {
		private static final long serialVersionUID = 6418831877479036414L;
		{
			add(NorthstarEventType.LOGGED_IN);
			add(NorthstarEventType.LOGGING_IN);
			add(NorthstarEventType.LOGGED_OUT);
			add(NorthstarEventType.LOGGING_OUT);
			add(NorthstarEventType.ACCOUNT);
			add(NorthstarEventType.POSITION);
			add(NorthstarEventType.TRADE);
			add(NorthstarEventType.ORDER);
		}
	};
	
	public AccountHandler(Map<String, TradeDayAccount> accountMap, TradeDayAccountFactory factory) {
		this.accountMap = accountMap;
		this.factory = factory;
	}
	
	@Override
	public void doHandle(NorthstarEvent e) {
		if(e.getEvent() == NorthstarEventType.LOGGED_IN) {
			String gatewayId = (String) e.getData();
			accountMap.put(gatewayId, factory.newInstance(gatewayId));
			log.info("账户登陆：{}", gatewayId);
		} else if (e.getEvent() == NorthstarEventType.LOGGED_OUT) {
			String gatewayId = (String) e.getData();
			accountMap.remove(gatewayId);
			log.info("账户登出：{}", gatewayId);
		} else if (e.getEvent() == NorthstarEventType.ACCOUNT) {
			AccountField af = (AccountField) e.getData();
			TradeDayAccount account = accountMap.get(af.getGatewayId());
			account.onAccountUpdate(af);
		} else if (e.getEvent() == NorthstarEventType.POSITION) {
			PositionField pf = (PositionField) e.getData();
			TradeDayAccount account = accountMap.get(pf.getGatewayId());
			account.onPositionUpdate(pf);
		} else if (e.getEvent() == NorthstarEventType.TRADE) {
			TradeField tf = (TradeField) e.getData();
			TradeDayAccount account = accountMap.get(tf.getGatewayId());
			account.onTradeUpdate(tf);
		} else if (e.getEvent() == NorthstarEventType.ORDER) {
			OrderField of = (OrderField) e.getData();
			TradeDayAccount account = accountMap.get(of.getGatewayId());
			account.onOrderUpdate(of);
		}
	}

	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return TARGET_TYPE.contains(eventType);
	}
	
}
