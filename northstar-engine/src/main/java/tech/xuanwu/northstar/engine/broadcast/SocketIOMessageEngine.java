package tech.xuanwu.northstar.engine.broadcast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.event.NorthstarEvent;

/**
 * 消息引擎
 * @author KevinHuangwl
 *
 */
@Slf4j
public class SocketIOMessageEngine {
	SocketIOServer server;
	
	public SocketIOMessageEngine(SocketIOServer server) {
		this.server = server;
	}
	
	/****************************************************/
	/*					消息发送端					  	*/
	/****************************************************/
	
	public void emitEvent(NorthstarEvent event, Class<?> objClz) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Method m = objClz.getMethod("toByteArray");
		server.getBroadcastOperations().sendEvent(event.getEvent().toString(), (byte[])m.invoke(event.getData()));
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
