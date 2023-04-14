package org.dromara.northstar.gateway.ctp;

import org.dromara.northstar.common.constant.FieldType;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.GatewaySettings;
import org.dromara.northstar.common.model.Setting;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CtpGatewaySettings extends DynamicParams implements GatewaySettings{
	
	@Setting(label = "网关账户", order = 10)
	private String userId;
	
	@Setting(label = "网关密码", type = FieldType.PASSWORD, order = 20)
	private String password;
	
	@Setting(label = "期货公司", type = FieldType.SELECT, options = {"平安主席", "宏源主席", "宏源次席"}, optionsVal = {"5200", "1080", "2070"}, placeholder = "请选择", order = 30)
	private String brokerId;

}
