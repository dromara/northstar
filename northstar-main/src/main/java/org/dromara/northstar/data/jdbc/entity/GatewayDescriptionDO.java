package org.dromara.northstar.data.jdbc.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

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
