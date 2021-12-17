package tech.quantit.northstar.main.engine.event.handler;

import java.util.EnumMap;

import tech.quantit.northstar.common.event.NorthstarEvent;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.event.FastEventEngine.NorthstarEventDispatcher;
import tech.quantit.northstar.main.handler.broadcast.SocketIOMessageEngine;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.NoticeField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

public class BroadcastDispatcher implements NorthstarEventDispatcher {
	
	private SocketIOMessageEngine msgEngine;
	
	private static EnumMap<NorthstarEventType, Class<?>> clzMap = new EnumMap<>(NorthstarEventType.class) {
		private static final long serialVersionUID = 1L;
		{
			put(NorthstarEventType.TICK, TickField.class);
			put(NorthstarEventType.BAR, BarField.class);
			put(NorthstarEventType.ACCOUNT, AccountField.class);
			put(NorthstarEventType.ORDER, OrderField.class);
			put(NorthstarEventType.POSITION, PositionField.class);
			put(NorthstarEventType.TRADE, TradeField.class);
			put(NorthstarEventType.NOTICE, NoticeField.class);
		}
	};
	
	public BroadcastDispatcher(SocketIOMessageEngine msgEngine) {
		this.msgEngine = msgEngine;
	}

	@Override
	public void onEvent(NorthstarEvent event, long sequence, boolean endOfBatch) throws Exception {
		NorthstarEventType type = event.getEvent();
		Class<?> clz = clzMap.get(type);
		if(clz == null) {
			return;
		}
		msgEngine.emitEvent(event, clz);
	}

}
