package tech.xuanwu.northstar.persistence.po;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 网关信息
 * @author KevinHuangwl
 *
 */
@Document
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GatewayPO {

	@Id
	private String gatewayId;
	
	private String description;
	
	private GatewayType gatewayType;
	
	private GatewayUsage gatewayUsage;
	
	private String gatewayAdapterType;
	
	private Object settings;

	public static enum GatewayType {
		CTP(CtpSettings.class),
		IB(IbSettings.class);
		
		private Class<?> clz;
		private GatewayType(Class<?> settingType) {
			this.clz = settingType;
		}
		
		public Class<?> getSettingType(){
			return clz;
		}
	}
	
	public static enum GatewayUsage {
		MARKET_DATA,
		TRADE;
	}
	
	@Data
	public static class CtpSettings {
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
	
	@Data
	public static class IbSettings {
		private String host;
		private int port;
		private int clientId;
	}
}
