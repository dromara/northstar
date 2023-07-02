package org.dromara.northstar.common.utils;

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

	public static String getPCName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			return "unknowHost";
		}
	}
	
	public static String getMACAddress() {
		try {
			byte[] hardwareAddress = getMACAddressBytes();
			String[] hexadecimalFormat = new String[hardwareAddress.length];
	        for (int i = 0; i < hardwareAddress.length; i++) {
	            hexadecimalFormat[i] = String.format("%02X", hardwareAddress[i]);
	        }
	        return String.join("-", hexadecimalFormat);
		} catch (Exception e) {
			log.warn("无法获取MAC地址", e);
			return "";
		}
	}
	
	private static byte[] getMACAddressBytes() throws SocketException, UnknownHostException {
		byte[] hardwareAddress = NetworkInterface.getByInetAddress(InetAddress.getLocalHost()).getHardwareAddress();
		if(hardwareAddress != null) {
			return hardwareAddress; 
		}
		
		Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
		while (networkInterfaces.hasMoreElements()) {
		    NetworkInterface ni = networkInterfaces.nextElement();
		    hardwareAddress = ni.getHardwareAddress();
		    if (hardwareAddress != null) {
		        return hardwareAddress;
		    }
		}
		throw new IllegalStateException("没有查到MAC信息");
	}
	
	
	private static Environment env;
	
	public static void setEnvironment(Environment env) {
		LocalEnvUtils.env = env;
	}
	
	public static Environment getEnvironment() {
		return LocalEnvUtils.env;
	}
	
}
