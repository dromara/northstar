package tech.xuanwu.northstar.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class SimpleContractInfo {

	private String unifiedSymbol; 
	
	private String name;
	
	private String gatewayId;
}
