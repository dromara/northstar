package tech.quantit.northstar.main.persistence.po;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.quantit.northstar.common.constant.GatewayType;

@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractPO {

	@Id
	private String unifiedSymbol;
	
	private byte[] data;
	
	private GatewayType gatewayType;
	
	private long updateTime;
}
