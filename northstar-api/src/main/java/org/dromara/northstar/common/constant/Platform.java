package org.dromara.northstar.common.constant;

public enum Platform {

	WINDOWS,
	
	LINUX,
	
	MAC_OS,
	
	UNKNOWN;
	
	public static Platform current() {
		String sysName = System.getProperties().getProperty("os.name").toUpperCase();
		if(sysName.contains("WINDOWS")) {
			return WINDOWS;
		}
		if(sysName.contains("LINUX")) {
			return LINUX;
		}
		if(sysName.contains("MAC")) {
			return MAC_OS;
		}
		return UNKNOWN;
	}
}
