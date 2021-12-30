package tech.quantit.northstar.main.handler.broadcast;

import java.util.HashMap;
import java.util.Map;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import com.google.protobuf.Message;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.event.NorthstarEvent;
import tech.quantit.northstar.common.utils.MessagePrinter;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 消息引擎
 * @author KevinHuangwl
 *
 */
@Slf4j
public class SocketIOMessageEngine {
	SocketIOServer server;
	
	/**
	 * 防止信道堵塞而增加的过滤机制
	 */
	private Map<String, Long> filterMap = new HashMap<>();
	
	public SocketIOMessageEngine(SocketIOServer server) {
		this.server = server;
	}
	
	/****************************************************/
	/*					消息发送端					  	*/
	/****************************************************/
	
	public void emitEvent(NorthstarEvent event) throws SecurityException, IllegalArgumentException {
		// 为了避免接收端信息拥塞，把行情数据按合约分房间分发数据，可以提升客户端的接收效率
		if(event.getData() instanceof TickField tick) {
			server.getRoomOperations(tick.getUnifiedSymbol()).sendEvent(event.getEvent().toString(), tick.toByteArray());
		} else if(event.getData() instanceof BarField bar) {
			server.getRoomOperations(bar.getUnifiedSymbol()).sendEvent(event.getEvent().toString(), bar.toByteArray());
		} else if(event.getData() instanceof AccountField account) {
			if(filterMap.containsKey(account.getAccountId()) && System.currentTimeMillis() - filterMap.get(account.getAccountId()) < 1000) {
				return;
			}
			log.trace("账户信息分发: [{}]", MessagePrinter.print(account));
			filterMap.put(account.getAccountId(), System.currentTimeMillis());
			server.getBroadcastOperations().sendEvent(event.getEvent().toString(), account.toByteArray());
		} else if(event.getData() instanceof PositionField position) {
			if(filterMap.containsKey(position.getPositionId()) && System.currentTimeMillis() - filterMap.get(position.getPositionId()) < 1000) {
				return;
			}
			filterMap.put(position.getPositionId(), System.currentTimeMillis());
			log.trace("持仓信息分发: [{}]", MessagePrinter.print(position));
			server.getBroadcastOperations().sendEvent(event.getEvent().toString(), position.toByteArray());
		} else if(event.getData() instanceof Message message) {			
			server.getBroadcastOperations().sendEvent(event.getEvent().toString(), message.toByteArray());
		}
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
