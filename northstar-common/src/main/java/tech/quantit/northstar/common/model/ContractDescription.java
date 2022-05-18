package tech.quantit.northstar.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ContractDescription {

	private String unifiedSymbol;
	
	private String symbol;
	
	private String name;
	
	private ProductClassEnum type;
	
	private String gatewayId;
}
