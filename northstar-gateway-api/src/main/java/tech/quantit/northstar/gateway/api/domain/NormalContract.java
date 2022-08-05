package tech.quantit.northstar.gateway.api.domain;

import lombok.NoArgsConstructor;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
import xyz.redtorch.pb.CoreField.ContractField;

/**
 * 
 * @author KevinHuangwl
 *
 */
@NoArgsConstructor
public class NormalContract {

	protected ContractField field;
	protected String gatewayType;
	protected long updateTime;

	public NormalContract(ContractField field, long updateTime) {
		this.field = field;
		this.gatewayType = field.getThirdPartyId().split("@")[1];
		this.updateTime = updateTime;
	}
	
	public String gatewayType() {
		return gatewayType;
	}
	
	public long updateTime() {
		return updateTime;
	}

	public String unifiedSymbol() {
		return field.getUnifiedSymbol();
	}
	
	public ProductClassEnum productClass() {
		return field.getProductClass();
	}

	public ContractField contractField() {
		return field;
	}

	public BarGenerator barGenerator() {
		return new BarGenerator(this);
	}

}
