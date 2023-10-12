package org.dromara.northstar.common.utils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import org.springframework.core.env.Environment;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LocalEnvUtils {

	private LocalEnvUtils() {
	}
	
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

	public static String getPCName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			return "unknowHost";
		}
	}
	
	private static byte[] getMACAddressBytes() throws SocketException {
		Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
		while (networkInterfaces.hasMoreElements()) {
		    NetworkInterface ni = networkInterfaces.nextElement();
		    byte[] hardwareAddress = ni.getHardwareAddress();
		    if (hardwareAddress != null) {
		        return hardwareAddress;
		    }
		}
		throw new IllegalStateException("没有查到MAC信息");
	}
	
	public static String getMACAddress() {
		String macAddr = "";
		try {
			byte[] hardwareAddress = getMACAddressBytes();
			String[] hexadecimalFormat = new String[hardwareAddress.length];
	        for (int i = 0; i < hardwareAddress.length; i++) {
	            hexadecimalFormat[i] = String.format("%02X", hardwareAddress[i]);
	        }
	        macAddr = String.join("-", hexadecimalFormat);
		} catch (Exception e) {
			log.warn("", e);
		}
		
		log.info("当前系统MAC地址：{}", macAddr);
		return macAddr;
	}
	
	private static Environment env;
	
	public static void setEnvironment(Environment env) {
		LocalEnvUtils.env = env;
	}
	
	public static Environment getEnvironment() {
		return LocalEnvUtils.env;
	}
	
}
