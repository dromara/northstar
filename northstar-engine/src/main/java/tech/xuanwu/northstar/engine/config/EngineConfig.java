package tech.xuanwu.northstar.engine.config;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.corundumstudio.socketio.SocketIOServer;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.constant.Constants;
import tech.xuanwu.northstar.common.event.InternalEventBus;
import tech.xuanwu.northstar.engine.broadcast.SocketIOMessageEngine;
import tech.xuanwu.northstar.engine.event.DisruptorFastEventEngine;
import tech.xuanwu.northstar.engine.event.DisruptorFastEventEngine.WaitStrategyEnum;
import tech.xuanwu.northstar.engine.event.EventEngine;
import xyz.redtorch.pb.CoreField.ContractField;

/**
 * 引擎配置
 * @author KevinHuangwl
 *
 */
@Slf4j
@Configuration
public class EngineConfig {

	@Value("${socketio.host}")
    private String host;
	
	@Value("${socketio.port}")
    private int port;

    @Bean
    public SocketIOServer socketIOServer() throws IOException {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname(host);
        config.setPort(port);
        config.setBossThreads(1);
        config.setWorkerThreads(100);
        log.info("WebSocket服务地址：{}:{}", host, port);
        SocketIOServer socketServer = new SocketIOServer(config);
        socketServer.start();
        return socketServer;
    }
	
	@Autowired
	@Bean
	public SocketIOMessageEngine createMessageEngine(SocketIOServer server) {
		return new SocketIOMessageEngine(server);
	}
	
	@Bean
	public EventEngine createEventEngine() {
		return new DisruptorFastEventEngine(WaitStrategyEnum.BlockingWaitStrategy);
	}
	
	@Bean
	public InternalEventBus createEventBus() {
		return new InternalEventBus();
	}
	
	@Bean(Constants.GATEWAY_CONTRACT_MAP)
	public Map<String, Map<String, ContractField>> createContractMap(){
		return new ConcurrentHashMap<>();
	}
}
