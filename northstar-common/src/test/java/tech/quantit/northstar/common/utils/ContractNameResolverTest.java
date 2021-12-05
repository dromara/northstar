package tech.quantit.northstar.common.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ContractNameResolverTest {

	@Test
	void testGetCNSymbolName() {
		assertThat(ContractNameResolver.getCNSymbolName("rb2210")).isEqualTo("螺纹钢2210");
		assertThat(ContractNameResolver.getCNSymbolName("rb0000")).isEqualTo("螺纹钢0000");
		assertThat(ContractNameResolver.getCNSymbolName("rb")).isEqualTo("螺纹钢");
	}

	@Test
	void testSymbolToSymbolGroup() {
		assertThat(ContractNameResolver.symbolToSymbolGroup("rb2210")).isEqualTo("rb");
	}

	@Test
	void testUnifiedSymbolToSymbolGroup() {
		assertThat(ContractNameResolver.unifiedSymbolToSymbolGroup("rb2210@SHFE@FUTURES")).isEqualTo("rb");
	}

}
