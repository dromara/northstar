package tech.xuanwu.northstar.engine.event.handler;

import java.util.EnumMap;

import org.springframework.beans.factory.InitializingBean;

import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.engine.broadcast.SocketIOMessageEngine;
import tech.xuanwu.northstar.engine.event.EventEngine;
import tech.xuanwu.northstar.engine.event.EventEngine.NorthstarEventHandler;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.NoticeField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

public class BroadcastHandler implements NorthstarEventHandler, InitializingBean {
	
	private SocketIOMessageEngine msgEngine;
	private EventEngine ee;
	
	private static EnumMap<NorthstarEventType, Class<?>> clzMap = new EnumMap<>(NorthstarEventType.class);
	
	static {
		clzMap.put(NorthstarEventType.TICK, TickField.class);
		clzMap.put(NorthstarEventType.BAR, BarField.class);
		clzMap.put(NorthstarEventType.ACCOUNT, AccountField.class);
		clzMap.put(NorthstarEventType.BALANCE, AccountField.class);
		clzMap.put(NorthstarEventType.ORDER, OrderField.class);
		clzMap.put(NorthstarEventType.POSITION, PositionField.class);
		clzMap.put(NorthstarEventType.TRADE, TradeField.class);
		clzMap.put(NorthstarEventType.NOTICE, NoticeField.class);
	}
	
	public BroadcastHandler(EventEngine ee, SocketIOMessageEngine msgEngine) {
		this.msgEngine = msgEngine;
		this.ee = ee;
	}

	@Override
	public void onEvent(NorthstarEvent event, long sequence, boolean endOfBatch) throws Exception {
		NorthstarEventType type = event.getEvent();
		Class<?> clz = clzMap.get(type);
		if(clz != null) {
			msgEngine.emitEvent(event, clz);
			return;
		}
		
		if(type == NorthstarEventType.CONNECTED || type == NorthstarEventType.CONNECTING
				|| type == NorthstarEventType.DISCONNECTED || type == NorthstarEventType.DISCONNECTING
				|| type == NorthstarEventType.LOGGED_IN || type == NorthstarEventType.LOGGING_IN
				|| type == NorthstarEventType.LOGGED_OUT || type == NorthstarEventType.LOGGING_OUT
				|| type == NorthstarEventType.TRADE_DATE) {
			msgEngine.emitMessageEvent(event);
			return;
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		ee.addHandler(this);
	}

}
