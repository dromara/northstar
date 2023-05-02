package org.dromara.northstar.data.jdbc.model;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.dromara.northstar.common.model.GatewayDescription;

import com.alibaba.fastjson2.JSON;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
public class GatewayDescriptionDO {

	@Id
	private String gatewayId;
	
	private String dataStr;
	
	public static GatewayDescriptionDO convertFrom(GatewayDescription gd) {
		return new GatewayDescriptionDO(gd.getGatewayId(), JSON.toJSONString(gd));
	}
	
	public GatewayDescription convertTo() {
		return JSON.parseObject(dataStr, GatewayDescription.class);
	}
}
