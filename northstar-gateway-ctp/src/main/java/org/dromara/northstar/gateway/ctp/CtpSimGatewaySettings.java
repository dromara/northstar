package org.dromara.northstar.gateway.ctp;

import lombok.Getter;
import lombok.Setter;
import tech.quantit.northstar.common.constant.FieldType;
import tech.quantit.northstar.common.model.DynamicParams;
import tech.quantit.northstar.common.model.GatewaySettings;
import tech.quantit.northstar.common.model.Setting;

@Getter
@Setter
public class CtpSimGatewaySettings extends DynamicParams implements GatewaySettings {

	@Setting(label = "网关账户", order = 10)
	private String userId;
	
	@Setting(label = "网关密码", type = FieldType.PASSWORD, order = 20)
	private String password;
	
	@Setting(label = "期货公司", type = FieldType.SELECT, options = {"宏源仿真"}, optionsVal = {"3070"}, placeholder = "请选择", order = 30)
	private String brokerId;

}
