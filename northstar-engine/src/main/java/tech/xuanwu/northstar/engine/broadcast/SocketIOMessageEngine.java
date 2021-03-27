package tech.xuanwu.northstar.engine.broadcast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.EnumMap;

import org.springframework.beans.factory.InitializingBean;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.engine.event.EventEngine;
import tech.xuanwu.northstar.engine.event.EventEngine.Event;
import tech.xuanwu.northstar.engine.event.EventEngine.NorthstarEventHandler;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.NoticeField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 消息引擎
 * @author KevinHuangwl
 *
 */
@Slf4j
public class SocketIOMessageEngine implements NorthstarEventHandler, InitializingBean{
	
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
	
	public SocketIOMessageEngine(EventEngine ee, SocketIOServer server) {
		this.ee = ee;
		this.server = server;
	}
	
	@Override
	public void onEvent(Event event, long sequence, boolean endOfBatch) throws Exception {
		NorthstarEventType type = event.getEvent();
		Class<?> clz = clzMap.get(type);
		if(clz != null) {
			emitEvent(event, clz);
			return;
		}
		
		if(type == NorthstarEventType.CONNECTED || type == NorthstarEventType.CONNECTING
				|| type == NorthstarEventType.DISCONNECTED || type == NorthstarEventType.DISCONNECTING
				|| type == NorthstarEventType.LOGINED || type == NorthstarEventType.LOGINING
				|| type == NorthstarEventType.TRADE_DATE) {
			emitMessageEvent(event);
			return;
		}
	}
	
	/**************************************************/
	/*					消息发送端						  */
	/**************************************************/
	SocketIOServer server;
	
	private void emitEvent(Event event, Class<?> objClz) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Method m = objClz.getMethod("toByteArray");
		server.getBroadcastOperations().sendEvent(event.getEvent().toString(), (byte[])m.invoke(event.getObj()));
	}
	
	private void emitMessageEvent(Event event) {
		server.getBroadcastOperations().sendEvent(event.getEvent().toString(), event.getObj().toString());
	}
	
	
	/**************************************************/
	/*					消息接收端						  */
	/**************************************************/
	@OnConnect  
    private void onConnect(final SocketIOClient client) {
    	log.info("【客户端连接】-[{}],建立连接", client.getSessionId());
    }  
  
    @OnDisconnect  
    private void onDisconnect(final SocketIOClient client) {
    	log.info("【客户端断开】-[{}],断开连接", client.getSessionId());
    }
    
    @OnEvent("login")
    private void login(final SocketIOClient client, String room) {
    	log.info("【登陆房间】-[{}]加入房间{}", client.getSessionId(), room);
    	client.joinRoom(room);
    }
    
    @OnEvent("logout")
    private void logout(final SocketIOClient client, String room) {
    	log.info("【离开房间】-[{}]离开房间{}", client.getSessionId(), room);
    	client.leaveRoom(room);
    }

	@Override
	public void afterPropertiesSet() throws Exception {
		ee.addHandler(this);
	}

}
