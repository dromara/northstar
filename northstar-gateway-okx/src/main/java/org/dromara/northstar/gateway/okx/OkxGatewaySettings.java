package org.dromara.northstar.gateway.okx;

import org.dromara.northstar.common.constant.FieldType;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.GatewaySettings;
import org.dromara.northstar.common.model.Setting;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OkxGatewaySettings extends DynamicParams implements GatewaySettings{

	@Setting(label="API Key", order=10, type=FieldType.TEXT)
	private String apiKey;
	
	@Setting(label="Secret Key", order=20, type=FieldType.TEXT)
	private String secretKey;
}
