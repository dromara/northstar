package tech.quantit.northstar.main.config;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

import javax.net.ssl.SSLHandshakeException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import com.corundumstudio.socketio.SocketConfig;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.SpringAnnotationScanner;
import com.corundumstudio.socketio.listener.ExceptionListener;

import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.main.utils.InetAddressUtils;

@Slf4j
@Configuration
public class SocketIOServerConfig implements InitializingBean {
	
	@Value("${server.ssl.key-store-type:}")
	private String keyStoreFormat;
	
	@Value("${server.ssl.key-store-password:}")
	private String keyStorePassword;
	
	@Value("${server.ssl.key-store:}")
	private Resource keyStore;
	
	@Value("${server.ssl.enabled}")
	private boolean sslEnabled;

	private UserInfo userInfo = new UserInfo();
	
	@Bean
	@ConditionalOnExpression("!'${spring.profiles.active}'.equals('test')")
    public SocketIOServer socketIOServer() {
		try {
			return makeServer();
		} catch (IOException e) {
			throw new RuntimeException("请检查本机网络环境是否正常", e);
		}
    }
	
	@Bean
	public UserInfo userInfo() {
		return userInfo;
	}
	
	private SocketIOServer makeServer() throws IOException {
		String token = Base64.getEncoder().encodeToString(String.format("%s:%s", userInfo.getUserId(), userInfo.getPassword()).getBytes());
		String realHost = InetAddressUtils.getInet4Address();
		com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname(realHost);
        config.setPort(51888);
        if(sslEnabled){
        	config.setKeyStore(keyStore.getInputStream());
        	config.setKeyStoreFormat(keyStoreFormat);
        	config.setKeyStorePassword(keyStorePassword);
        }
        config.setAuthorizationListener(data -> data.getUrlParams().get("auth").get(0).equals(token));
        config.setBossThreads(1);
        config.setWorkerThreads(100);
        SocketConfig socketConfig = new SocketConfig();
        socketConfig.setReuseAddress(true);
        config.setSocketConfig(socketConfig);
        config.setExceptionListener(new ExceptionListener() {
			
			@Override
			public void onPingException(Exception e, SocketIOClient client) {
				log.warn("Ping exception:", e);				
			}
			
			@Override
			public void onEventException(Exception e, List<Object> args, SocketIOClient client) {
				log.warn("Event exception:", e);				
			}
			
			@Override
			public void onDisconnectException(Exception e, SocketIOClient client) {
				log.warn("Disconnect exception:", e);				
			}
			
			@Override
			public void onConnectException(Exception e, SocketIOClient client) {
				log.warn("Connect exception:", e);
			}
			
			@Override
			public boolean exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
				if(!(e.getCause() instanceof SSLHandshakeException)) {
					// 忽略SSLHandshakeException
					log.warn("", e);
				}
				return true;
			}
		});
        log.info("WebSocket服务地址：{}:{}", realHost, 51888);
        SocketIOServer server = new SocketIOServer(config);
        server.start();
        return server;
	}
	
	@Bean
	public SpringAnnotationScanner springAnnotationScanner(SocketIOServer socketServer) {
		return new SpringAnnotationScanner(socketServer);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		log.info("自动装配SocketIOServerAutoConfiguration");
	}
}
