package org.dromara.northstar.data.jdbc.entity;

import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.support.utils.CodecUtils;

import com.alibaba.fastjson2.JSON;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
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
	private String dataEncoded;
	
	public static GatewayDescriptionDO convertFrom(GatewayDescription gd) {
		return GatewayDescriptionDO.builder()
				.gatewayId(gd.getGatewayId())
				.dataEncoded(CodecUtils.encrypt(JSON.toJSONString(gd)))
				.build();
	}
	
	public GatewayDescription convertTo() {
		return JSON.parseObject(CodecUtils.decrypt(dataEncoded), GatewayDescription.class);
	}
}
