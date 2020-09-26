package tech.xuanwu.northstar.trader.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.SpringAnnotationScanner;

import lombok.extern.slf4j.Slf4j;

/**
 * SocketIO服务配置
 * @author kevinhuangwl
 *
 */
@Configuration
@Slf4j
public class SocketIOServerConfig {
	
	@Value("${socketio.host}")
    private String host;
	
	@Value("${socketio.port}")
    private int port;

    private int bossCount = 1;

    private int workCount = 100;

    @Bean
    public SocketIOServer socketIOServer() throws IOException {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname(host);
        config.setPort(port);
        config.setBossThreads(bossCount);
        config.setWorkerThreads(workCount);
        log.info("WebSocket服务地址：{}:{}", host, port);
        SocketIOServer socketServer = new SocketIOServer(config);
        socketServer.start();
        return socketServer;
    }
    
    @Bean
	public SpringAnnotationScanner springAnnotationScanner(SocketIOServer socketServer) {
		return new SpringAnnotationScanner(socketServer);
	}
}
