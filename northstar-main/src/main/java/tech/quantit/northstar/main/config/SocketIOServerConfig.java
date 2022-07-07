package tech.quantit.northstar.main.config;

import java.net.SocketException;
import java.util.Base64;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.corundumstudio.socketio.SocketConfig;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.SpringAnnotationScanner;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.main.utils.InetAddressUtils;

@Slf4j
@Configuration
public class SocketIOServerConfig implements DisposableBean, InitializingBean {

	@Value("${socketio.host}")
    private String host;
	
	@Value("${socketio.port}")
    private int port;
	
	@Autowired
	private SocketIOServer socketServer;
	
	private UserInfo userInfo = new UserInfo();
	
	@Bean
	@ConditionalOnExpression("!'${spring.profiles.active}'.equals('test')")
    public SocketIOServer socketIOServer() throws SocketException  {
		return makeServer();
    }
	
	@Bean
	public UserInfo userInfo() {
		return userInfo;
	}
	
	private SocketIOServer makeServer() throws SocketException {
		String token = Base64.getEncoder().encodeToString(String.format("%s:%s", userInfo.getUserId(), userInfo.getPassword()).getBytes());
		String realHost = StringUtils.equals(host, "0.0.0.0") ? InetAddressUtils.getInet4Address() : host;
		com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname(realHost);
        config.setPort(port);
        config.setAuthorizationListener(data -> data.getUrlParams().get("auth").get(0).equals(token));
        config.setBossThreads(1);
        config.setWorkerThreads(100);
        SocketConfig socketConfig = new SocketConfig();
        socketConfig.setReuseAddress(true);
        config.setSocketConfig(socketConfig);
        log.info("WebSocket服务地址：{}:{}", realHost, port);
        SocketIOServer server = new SocketIOServer(config);
        server.start();
        return server;
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
