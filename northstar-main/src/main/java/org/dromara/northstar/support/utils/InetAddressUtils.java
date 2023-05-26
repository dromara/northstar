package org.dromara.northstar.support.utils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InetAddressUtils {
	
	private InetAddressUtils() {}

	public static String getInet4Address() throws SocketException {
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
	
	public static String getHostname() {
		String hostname = "Unknown";
		try {
            // 获取本地主机的 InetAddress 实例
            InetAddress localHost = InetAddress.getLocalHost();
            // 获取主机名
            hostname = localHost.getHostName();
        } catch (UnknownHostException e) {
            log.error("无法获取主机名: " + e.getMessage());
        }
		return hostname;
	}
}
