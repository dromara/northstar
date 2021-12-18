package xyz.redtorch.gateway.ctp.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CtpContractNameResolverTest {

	@Test
	public void testGetCNSymbolName() {
		assertThat(CtpContractNameResolver.getCNSymbolName("rb2210")).isEqualTo("螺纹钢2210");
		assertThat(CtpContractNameResolver.getCNSymbolName("rb0000")).isEqualTo("螺纹钢0000");
		assertThat(CtpContractNameResolver.getCNSymbolName("rb")).isEqualTo("螺纹钢");
	}

	@Test
	public void testSymbolToSymbolGroup() {
		assertThat(CtpContractNameResolver.symbolToSymbolGroup("rb2210")).isEqualTo("rb");
	}

	@Test
	public void testUnifiedSymbolToSymbolGroup() {
		assertThat(CtpContractNameResolver.unifiedSymbolToSymbolGroup("rb2210@SHFE@FUTURES")).isEqualTo("rb");
	}

}
