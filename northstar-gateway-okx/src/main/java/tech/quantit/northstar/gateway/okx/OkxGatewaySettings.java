package tech.quantit.northstar.gateway.okx;

import lombok.Getter;
import lombok.Setter;
import tech.quantit.northstar.common.constant.FieldType;
import tech.quantit.northstar.common.model.DynamicParams;
import tech.quantit.northstar.common.model.GatewaySettings;
import tech.quantit.northstar.common.model.Setting;

@Getter
@Setter
public class OkxGatewaySettings extends DynamicParams implements GatewaySettings{

	@Setting(label="API Key", order=10, type=FieldType.TEXT)
	private String apiKey;
	
	@Setting(label="Secret Key", order=20, type=FieldType.TEXT)
	private String secretKey;
}
