package tech.xuanwu.northstar.main.config;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.corundumstudio.socketio.SocketConfig;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.SpringAnnotationScanner;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class SocketIOServerConfig implements DisposableBean, InitializingBean {

	@Value("${socketio.host}")
    private String host;
	
	@Value("${socketio.port}")
    private int port;
	
	@Autowired
	private SocketIOServer socketServer;
	
	@Bean
	@ConditionalOnProperty(value = "spring.profiles.active", havingValue = "prod")
    public SocketIOServer socketIOServer()  {
		return makeServer();
    }
	
	@Bean
	@ConditionalOnProperty(value = "spring.profiles.active", havingValue = "dev")
	public SocketIOServer socketIOServer2() {
        return makeServer();
    }
	
	private SocketIOServer makeServer() {
		com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname(host);
        config.setPort(port);
        config.setBossThreads(1);
        config.setWorkerThreads(100);
        SocketConfig socketConfig = new SocketConfig();
        socketConfig.setReuseAddress(true);
        config.setSocketConfig(socketConfig);
        log.info("WebSocket服务地址：{}:{}", host, port);
        SocketIOServer socketServer = new SocketIOServer(config);
        socketServer.start();
        return socketServer;
	}
	
	@Bean
	public SpringAnnotationScanner springAnnotationScanner(SocketIOServer socketServer) {
		return new SpringAnnotationScanner(socketServer);
	}

	@Override
	public void destroy() throws Exception {
		socketServer.stop();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		log.info("自动装配SocketIOServerAutoConfiguration");
	}
}
