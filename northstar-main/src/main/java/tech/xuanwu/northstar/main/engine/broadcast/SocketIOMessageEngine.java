package tech.xuanwu.northstar.main.engine.broadcast;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import com.google.protobuf.Message;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 消息引擎
 * @author KevinHuangwl
 *
 */
@Slf4j
public class SocketIOMessageEngine {
	SocketIOServer server;
	
	private Set<NorthstarEventType> mdType = new HashSet<>() {
		private static final long serialVersionUID = 1L;

		{
			add(NorthstarEventType.TICK);
			add(NorthstarEventType.IDX_TICK);
			add(NorthstarEventType.BAR);
			add(NorthstarEventType.HIS_BAR);
		}
	};
	
	public SocketIOMessageEngine(SocketIOServer server) {
		this.server = server;
	}
	
	/****************************************************/
	/*					消息发送端					  	*/
	/****************************************************/
	
	public void emitEvent(NorthstarEvent event, Class<?> objClz) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		NorthstarEventType type = event.getEvent();
		// 为了避免接收端信息拥塞，把行情数据按合约分房间分发数据，可以提升客户端的接收效率
		if(mdType.contains(type)) {
			if(type == NorthstarEventType.TICK || type == NorthstarEventType.IDX_TICK) {
				emitTickData(event);
			}else {
				emitBarData(event);
			}
			
			return;
		}
		Message message = (Message) event.getData();
		server.getBroadcastOperations().sendEvent(event.getEvent().toString(), message.toByteArray());
	}
	
	private void emitTickData(NorthstarEvent event) {
		TickField tick = (TickField) event.getData();
		server.getRoomOperations(tick.getUnifiedSymbol()).sendEvent(event.getEvent().toString(), tick.toByteArray());
	}
	
	private void emitBarData(NorthstarEvent event) {
		BarField bar = (BarField) event.getData();
		server.getRoomOperations(bar.getUnifiedSymbol()).sendEvent(event.getEvent().toString(), bar.toByteArray());
	}
	
	/****************************************************/
	/*					消息接收端						*/
	/****************************************************/
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
}
