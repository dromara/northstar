package tech.quantit.northstar.main.persistence.po;

import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.quantit.northstar.common.constant.GatewayType;

@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractPO {

	@Id
	private String unifiedSymbol;
	
	private byte[] data;
	
	private GatewayType gatewayType;
	
	private long updateTime;
	
	private boolean isIndexContract;
	
	private Set<String> monthlyContractSymbols;
}
