package org.dromara.northstar.event;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.dromara.northstar.common.event.AbstractEventHandler;
import org.dromara.northstar.common.event.GenericEventHandler;
import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.model.core.Account;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Notice;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Position;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.common.utils.CommonUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;

import cn.hutool.core.codec.Base64;
import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.TickField;

@Slf4j
public class BroadcastHandler extends AbstractEventHandler implements GenericEventHandler, InitializingBean, DisposableBean {
	
	private SocketIOServer socketServer;
	
	private ExecutorService exec;
	
	private static final Set<NorthstarEventType> TARGET_TYPE = EnumSet.of(
			NorthstarEventType.TICK, 
			NorthstarEventType.BAR,
			NorthstarEventType.ACCOUNT,
			NorthstarEventType.POSITION,
			NorthstarEventType.TRADE,
			NorthstarEventType.ORDER,
			NorthstarEventType.NOTICE
	); 
	
	public BroadcastHandler(SocketIOServer socketServer) {
		this.socketServer = socketServer;
	}
	
	/*****************************************************/
	/**					消息发送端					  		**/
	/*****************************************************/
	public void emitEvent(NorthstarEvent event) throws SecurityException, IllegalArgumentException, InterruptedException {
		// 为了避免接收端信息拥塞，把行情数据按合约分房间分发数据，可以提升客户端的接收效率
		if(event.getData() instanceof Tick t) {
			TickField tick = t.toTickField();
			String rmid = String.format("%s@%s", tick.getUnifiedSymbol(), tick.getGatewayId());
			log.trace("TICK数据分发：[{} {} {} 价格：{}]", tick.getUnifiedSymbol(), tick.getActionDay(), tick.getActionTime(), tick.getLastPrice());
			socketServer.getRoomOperations(rmid).sendEvent(NorthstarEventType.TICK.toString(), Base64.encode(tick.toByteArray()));
		} else if(event.getData() instanceof Bar b) {
			BarField bar = b.toBarField();
			String rmid = String.format("%s@%s", bar.getUnifiedSymbol(), bar.getGatewayId());
			log.trace("BAR数据分发：[{} {} {} 价格：{}]", rmid, bar.getActionDay(), bar.getActionTime(), bar.getClosePrice());
			socketServer.getRoomOperations(rmid).sendEvent(NorthstarEventType.BAR.toString(), Base64.encode(bar.toByteArray()));
		} else if(event.getData() instanceof Account acc) {
			AccountField account = acc.toAccountField();
			log.trace("账户信息分发: [{} {} {}]", account.getAccountId(), account.getGatewayId(), account.getBalance());
			socketServer.getBroadcastOperations().sendEvent(event.getEvent().toString(), Base64.encode(account.toByteArray()));
		} else if(event.getData() instanceof Position pos) {
			PositionField position = pos.toPositionField();
			log.trace("持仓信息分发: [{} {} {}]", position.getAccountId(), position.getGatewayId(), position.getPositionId());
			socketServer.getBroadcastOperations().sendEvent(event.getEvent().toString(), Base64.encode(position.toByteArray()));
		} else if(event.getData() instanceof Order od) {
			socketServer.getBroadcastOperations().sendEvent(event.getEvent().toString(), Base64.encode(od.toOrderField().toByteArray()));
		} else if(event.getData() instanceof Trade tr) {
			socketServer.getBroadcastOperations().sendEvent(event.getEvent().toString(), Base64.encode(tr.toTradeField().toByteArray()));
		} else if(event.getData() instanceof Notice note) {
			socketServer.getBroadcastOperations().sendEvent(event.getEvent().toString(), Base64.encode(note.toNoticeField().toByteArray()));
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

	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return TARGET_TYPE.contains(eventType);
	}

	@Override
	protected void doHandle(NorthstarEvent e) {
		exec.execute(() -> {
			try {
				BroadcastHandler.this.emitEvent(e);
			} catch (SecurityException | IllegalArgumentException | InterruptedException ex) {
				log.error("数据分发异常", ex);
			}
		});
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		exec = CommonUtils.newThreadPerTaskExecutor(getClass());
	}
	
	@Override
	public void destroy() throws Exception {
		exec.close();
	}
}
