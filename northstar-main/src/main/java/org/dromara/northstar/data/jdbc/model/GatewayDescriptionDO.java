package org.dromara.northstar.data.jdbc.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.dromara.northstar.common.model.GatewayDescription;

import com.alibaba.fastjson2.JSON;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name="GATEWAY", indexes = {
		@Index(name="idx_gatewayId", columnList = "gatewayId")
})
public class GatewayDescriptionDO {

	@Id
	private String gatewayId;
	@Lob
	private String dataStr;
	
	public static GatewayDescriptionDO convertFrom(GatewayDescription gd) {
		return new GatewayDescriptionDO(gd.getGatewayId(), JSON.toJSONString(gd));
	}
	
	public GatewayDescription convertTo() {
		return JSON.parseObject(dataStr, GatewayDescription.class);
	}
}
