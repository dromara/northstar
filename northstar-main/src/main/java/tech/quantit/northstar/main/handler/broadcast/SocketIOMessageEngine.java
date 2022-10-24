package tech.quantit.northstar.main.handler.broadcast;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import com.google.protobuf.Message;

import cn.hutool.core.codec.Base64;
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
	
	public SocketIOMessageEngine(SocketIOServer server) {
		this.server = server;
	}
	
	/****************************************************/
	/*					消息发送端					  	*/
	/****************************************************/
	
	public void emitEvent(NorthstarEvent event) throws SecurityException, IllegalArgumentException {
		// 为了避免接收端信息拥塞，把行情数据按合约分房间分发数据，可以提升客户端的接收效率
		if(event.getData() instanceof TickField tick) {
			log.trace("TICK数据分发：[{} {} {} 价格：{}]", tick.getUnifiedSymbol(), tick.getActionDay(), tick.getActionTime(), tick.getLastPrice());
			server.getRoomOperations(tick.getUnifiedSymbol()).sendEvent(event.getEvent().toString(), Base64.encode(tick.toByteArray()));
		} else if(event.getData() instanceof BarField bar) {
			log.trace("BAR数据分发：[{} {} {} 价格：{}]", bar.getUnifiedSymbol(), bar.getActionDay(), bar.getActionTime(), bar.getClosePrice());
			server.getRoomOperations(bar.getUnifiedSymbol()).sendEvent(event.getEvent().toString(), Base64.encode(bar.toByteArray()));
		} else if(event.getData() instanceof AccountField account) {
			log.trace("账户信息分发: [{}]", MessagePrinter.print(account));
			server.getBroadcastOperations().sendEvent(event.getEvent().toString(), Base64.encode(account.toByteArray()));
		} else if(event.getData() instanceof PositionField position) {
			log.trace("持仓信息分发: [{}]", MessagePrinter.print(position));
			server.getBroadcastOperations().sendEvent(event.getEvent().toString(), Base64.encode(position.toByteArray()));
		} else if(event.getData() instanceof Message message) {			
			server.getBroadcastOperations().sendEvent(event.getEvent().toString(), Base64.encode(message.toByteArray()));
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
