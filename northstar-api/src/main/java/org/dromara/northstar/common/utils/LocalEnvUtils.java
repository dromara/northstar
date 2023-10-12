package org.dromara.northstar.common.utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.List;

import org.apache.commons.io.IOUtils;
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
		String macAddr = "";
		try {
			byte[] hardwareAddress = getMACAddressBytes();
			String[] hexadecimalFormat = new String[hardwareAddress.length];
	        for (int i = 0; i < hardwareAddress.length; i++) {
	            hexadecimalFormat[i] = String.format("%02X", hardwareAddress[i]);
	        }
	        macAddr = String.join("-", hexadecimalFormat);
		} catch (Exception e) {
			log.warn("无法通过JAVA接口获取MAC地址", e);
		}
		
		if(System.getProperties().getProperty("os.name").toUpperCase().indexOf("LINUX") != -1) {
			try {
				macAddr = getMACStringFromLinux();
			} catch (IOException e) {
				log.warn("无法通过Linux命令行获取MAC地址", e);
			}
		}
		
		return macAddr;
	}
	
	private static String getMACStringFromLinux() throws IOException {
		Process process = Runtime.getRuntime().exec("ifconfig | grep ether");
		List<String> lines = IOUtils.readLines(process.getInputStream(), StandardCharsets.UTF_8);
		if(lines.isEmpty()) {
			return "";
		}
		return lines.get(0).trim().split("\\s+")[1];
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
