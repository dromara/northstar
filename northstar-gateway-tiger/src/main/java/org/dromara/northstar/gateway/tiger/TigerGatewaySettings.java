package org.dromara.northstar.gateway.tiger;

import org.dromara.northstar.common.constant.FieldType;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.GatewaySettings;
import org.dromara.northstar.common.model.Setting;

import com.tigerbrokers.stock.openapi.client.struct.enums.License;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TigerGatewaySettings extends DynamicParams implements GatewaySettings {

	@Setting(label="用户ID", order=10, type=FieldType.TEXT)
	private String tigerId;
	
	@Setting(label="账户ID", order=20, type=FieldType.TEXT)
	private String accountId;
	
	@Setting(label="RSA私钥", order=30, type=FieldType.TEXT)
	private String privateKey;
	
	@Setting(label="证书类型", order=40, type=FieldType.SELECT, optionsVal = {"TBNZ", "TBSG"})
	private License license;
	
	@Setting(label="secretKey", order=50, type=FieldType.TEXT, required = false)
	private String secretKey;
	
}
