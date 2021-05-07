package tech.xuanwu.northstar.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.corundumstudio.socketio.SocketIOServer;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.event.InternalEventBus;
import tech.xuanwu.northstar.engine.broadcast.SocketIOMessageEngine;
import tech.xuanwu.northstar.engine.event.DisruptorFastEventEngine;
import tech.xuanwu.northstar.engine.event.DisruptorFastEventEngine.WaitStrategyEnum;
import tech.xuanwu.northstar.engine.event.EventEngine;

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
	
	@Bean
	public SocketIOMessageEngine createMessageEngine(SocketIOServer server) {
		log.info("创建SocketIOMessageEngine");
		return new SocketIOMessageEngine(server);
	}
	
	@Bean
	public EventEngine createEventEngine() {
		log.info("创建EventEngine");
		return new DisruptorFastEventEngine(WaitStrategyEnum.BlockingWaitStrategy);
	}
	
	@Bean
	public InternalEventBus createEventBus() {
		log.info("创建InternalEventBus");
		return new InternalEventBus();
	}
	
}
