package tech.xuanwu.northstar.trader.domain.broadcast;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.constant.MessageEvent;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.NoticeField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

@Slf4j
@Component
public class SocketIOMessageServer implements MessageEngine{
	
	/**************************************************/
	/*					消息发送端						  */
	/**************************************************/
	@Autowired
	SocketIOServer server;
	
	@Override
	public void emitTick(TickField tick) {
		server.getRoomOperations(tick.getUnifiedSymbol()).sendEvent(MessageEvent.TICK_DATA, tick.toByteArray());
	}
	
	@Override
	public void emitBar(BarField bar) {
		server.getRoomOperations(bar.getUnifiedSymbol()).sendEvent(MessageEvent.BAR_DATA, bar.toByteArray());
	}


	@Override
	public void emitAccount(AccountField account) {
		server.getRoomOperations(account.getGatewayId()).sendEvent(MessageEvent.ACCOUNT_DATA, account.toByteArray());
	}


	@Override
	public void emitPosition(PositionField position) {
		server.getRoomOperations(position.getGatewayId()).sendEvent(MessageEvent.POSITION_DATA, position.toByteArray());
	}


	@Override
	public void emitTrade(TradeField trade) {
		server.getRoomOperations(trade.getGatewayId()).sendEvent(MessageEvent.TRADE_DATA, trade.toByteArray());
	}


	@Override
	public void emitOrder(OrderField order) {
		server.getRoomOperations(order.getGatewayId()).sendEvent(MessageEvent.ORDER_DATA, order.toByteArray());
	}
	
	@Override
	public void emitContract(ContractField contract) {
		server.getRoomOperations(contract.getGatewayId()).sendEvent(MessageEvent.CONTRACT_DATA, contract.toByteArray());
	}
	
	@Override
	public void emitNotice(NoticeField notice) {
		server.getBroadcastOperations().sendEvent(MessageEvent.NOTICE_DATA, notice.toByteArray());
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

}
