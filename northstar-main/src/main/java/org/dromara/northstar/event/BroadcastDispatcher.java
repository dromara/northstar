package org.dromara.northstar.event;

import java.util.HashSet;
import java.util.Set;

import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.springframework.stereotype.Component;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.event.FastEventEngine.NorthstarEventDispatcher;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import com.google.protobuf.Message;

import cn.hutool.core.codec.Base64;
import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.TickField;

@Slf4j
@Component
public class BroadcastDispatcher implements NorthstarEventDispatcher {
	
	private SocketIOServer socketServer;
	
	private static Set<NorthstarEventType> eventSet = new HashSet<>() {
		private static final long serialVersionUID = 1L;
		{
			add(NorthstarEventType.TICK);
			add(NorthstarEventType.BAR);
			add(NorthstarEventType.ACCOUNT);
			add(NorthstarEventType.ORDER);
			add(NorthstarEventType.POSITION);
			add(NorthstarEventType.TRADE);
			add(NorthstarEventType.NOTICE);
		}
	};
	
	public BroadcastDispatcher(SocketIOServer socketServer, FastEventEngine feEngine) {
		this.socketServer = socketServer;
		feEngine.addHandler(this);
	}

	@Override
	public void onEvent(NorthstarEvent event, long sequence, boolean endOfBatch) throws Exception {
		if(!eventSet.contains(event.getEvent())) {
			return;
		}
		emitEvent(event);
	}

	/*****************************************************/
	/**					消息发送端					  		**/
	/*****************************************************/
	public void emitEvent(NorthstarEvent event) throws SecurityException, IllegalArgumentException, InterruptedException {
		// 为了避免接收端信息拥塞，把行情数据按合约分房间分发数据，可以提升客户端的接收效率
		if(event.getData() instanceof TickField tick) {
			String rmid = String.format("%s@%s", tick.getUnifiedSymbol(), tick.getGatewayId());
			log.trace("TICK数据分发：[{} {} {} 价格：{}]", tick.getUnifiedSymbol(), tick.getActionDay(), tick.getActionTime(), tick.getLastPrice());
			socketServer.getRoomOperations(rmid).sendEvent(NorthstarEventType.TICK.toString(), Base64.encode(tick.toByteArray()));
		} else if(event.getData() instanceof BarField bar) {
			String rmid = String.format("%s@%s", bar.getUnifiedSymbol(), bar.getGatewayId());
			log.trace("BAR数据分发：[{} {} {} 价格：{}]", rmid, bar.getActionDay(), bar.getActionTime(), bar.getClosePrice());
			socketServer.getRoomOperations(rmid).sendEvent(NorthstarEventType.BAR.toString(), Base64.encode(bar.toByteArray()));
		} else if(event.getData() instanceof AccountField account) {
			log.trace("账户信息分发: [{} {} {}]", account.getAccountId(), account.getGatewayId(), account.getBalance());
			socketServer.getBroadcastOperations().sendEvent(event.getEvent().toString(), Base64.encode(account.toByteArray()));
		} else if(event.getData() instanceof PositionField position) {
			log.trace("持仓信息分发: [{} {} {}]", position.getAccountId(), position.getGatewayId(), position.getPositionId());
			socketServer.getBroadcastOperations().sendEvent(event.getEvent().toString(), Base64.encode(position.toByteArray()));
		} else if(event.getData() instanceof Message message) {			
			socketServer.getBroadcastOperations().sendEvent(event.getEvent().toString(), Base64.encode(message.toByteArray()));
		}
	}
	
	/*************************************************/
	/**					消息接收端						**/
	/*************************************************/
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
