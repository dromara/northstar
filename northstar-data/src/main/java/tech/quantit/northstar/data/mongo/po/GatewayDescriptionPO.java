package tech.quantit.northstar.data.mongo.po;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import tech.quantit.northstar.common.model.GatewayDescription;

/**
 * 网关配置
 * @author KevinHuangwl
 *
 */
@Data
@Document
public class GatewayDescriptionPO {

	@Id
	private String gatewayId;
	
	private GatewayDescription gatewayDescription;
	
	public static GatewayDescriptionPO convertFrom(GatewayDescription gd) {
		GatewayDescriptionPO po = new GatewayDescriptionPO();
		po.gatewayId = gd.getGatewayId();
		po.gatewayDescription = gd;
		return po;
	}
}
