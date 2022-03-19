package tech.quantit.northstar.main.config;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

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
	@ConditionalOnExpression("!'${spring.profiles.active}'.equals('test')")
    public SocketIOServer socketIOServer() throws SocketException  {
		return makeServer();
    }
	
	private SocketIOServer makeServer() throws SocketException {
		String realHost = StringUtils.equals(host, "0.0.0.0") ? getInetAddress() : host;
		com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname(realHost);
        config.setPort(port);
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
	
	private String getInetAddress() throws SocketException {
		log.info("正在自动获取IP");
		Enumeration<NetworkInterface> allNetworkInterfaces = NetworkInterface.getNetworkInterfaces();
		while(allNetworkInterfaces.hasMoreElements()) {
			NetworkInterface netIntf = allNetworkInterfaces.nextElement();
			if(netIntf.isLoopback()||netIntf.isVirtual()||!netIntf.isUp()||netIntf.getDisplayName().contains("VM")){
                continue;
            }
			Enumeration<InetAddress> inetAddrs = netIntf.getInetAddresses();
			while(inetAddrs.hasMoreElements()) {
				InetAddress inetAddr = inetAddrs.nextElement();
				if(inetAddr instanceof Inet4Address inet4) {
					return inet4.getHostAddress();
				}
			}
		}
		throw new SocketException("没有找到IPv4的IP信息");
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
