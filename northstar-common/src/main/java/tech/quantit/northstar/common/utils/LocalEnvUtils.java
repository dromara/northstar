package tech.quantit.northstar.common.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LocalEnvUtils {

	private LocalEnvUtils() {
	}
	
	public static String getMACAddress() {
		try {
			InetAddress inet = InetAddress.getLocalHost();
			return getMACAddress(inet);
		} catch (Exception e) {
			log.warn("无法获取MAC地址", e);
			return "";
		}
	}

	private static String getMACAddress(InetAddress ia) throws Exception {
		byte[] mac = NetworkInterface.getByInetAddress(ia).getHardwareAddress();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < mac.length; i++) {
			if (i != 0) {
				sb.append("-");
			}
			String s = Integer.toHexString(mac[i] & 0xFF);
			sb.append(s.length() == 1 ? 0 + s : s);
		}
		return sb.toString().toUpperCase();
	}
	
}
