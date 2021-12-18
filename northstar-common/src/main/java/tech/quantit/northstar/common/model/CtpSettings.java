package tech.quantit.northstar.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class CtpSettings implements GatewaySettings{

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
