package tech.quantit.northstar.gateway.api.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
import xyz.redtorch.pb.CoreField.ContractField;

class NormalContractTest {
	
	private TestFieldFactory factory = new TestFieldFactory("gateway");
	
	ContractField contractField = factory.makeContract("rb2210");
	
	NormalContract contract = new NormalContract(contractField, 0); 


	@Test
	void testGatewayType() {
		assertThat(contract.gatewayType()).isEqualTo("CTP");
	}

	@Test
	void testUnifiedSymbol() {
		assertThat(contract.unifiedSymbol()).isEqualTo("rb2210@SHFE@FUTURES");
	}

	@Test
	void testContractField() {
		assertThat(contract.contractField()).isEqualTo(contractField);
	}

	@Test
	void testProductClass() {
		assertThat(contract.productClass()).isEqualTo(ProductClassEnum.FUTURES);
	}

}
