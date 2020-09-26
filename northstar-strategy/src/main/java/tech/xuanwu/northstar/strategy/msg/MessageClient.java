package tech.xuanwu.northstar.strategy.msg;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.protobuf.InvalidProtocolBufferException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter.Listener;
import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.constant.MessageEvent;
import tech.xuanwu.northstar.strategy.trade.Strategy;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 通信客户端，每个策略自己维护一个实例
 * @author kevinhuangwl
 *
 */
@Slf4j
@Component
public class MessageClient implements InitializingBean, DisposableBean{
	
	@Value("${northstar.message.endpoint}")
	private String msgServerEndpoint;
	
	List<Strategy> strategyList = new CopyOnWriteArrayList<>();
	
	Socket client;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		client = IO.socket(msgServerEndpoint);
		
		final Listener callback = (data) -> {
			log.info("消息客户端【{}】连线成功", client.id());
		};
		
		client.on(Socket.EVENT_RECONNECT, callback);
		
		client.on(Socket.EVENT_CONNECT, callback);
		
		client.on(MessageEvent.TICK_DATA, (data)->{
			byte[] b = (byte[]) data[0];
			try {
				TickField tick = TickField.parseFrom(b);
				onTick(tick);
			} catch (InvalidProtocolBufferException e) {
				log.error("Tick数据转换异常",e);
			}
		});
		
		client.connect();
		client.emit("login", "AP007@CZCE@FUTURES");
	}
	
	@Override
	public void destroy() throws Exception {
		client.disconnect();
	}
	
	/**
	 * 收到TICK数据
	 * @param tick
	 */
	private void onTick(TickField tick) {
		for(Strategy s : strategyList) {
			s.updateTick(tick);
		}
	}
	
	/**
	 * 注册策略
	 * @param s
	 */
	public void registerStrategy(Strategy s) {
		strategyList.add(s);
	}
}
