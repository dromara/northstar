package tech.xuanwu.northstar.model;

import lombok.Data;

@Data
public class CtpSettings {

	private String userId;
	private String password;
	private String brokerId;
	private String tdHost;
	private String tdPort;
	private String mdHost;
	private String mdPort;
	private String authCode;
	private String userProductInfo; 
	private String appId;
}
