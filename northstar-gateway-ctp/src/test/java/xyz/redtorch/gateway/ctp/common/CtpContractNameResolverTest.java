package xyz.redtorch.gateway.ctp.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CtpContractNameResolverTest {

	@Test
	public void testGetCNSymbolName() {
		assertThat(CtpContractNameResolver.getCNSymbolName("rb2210")).isEqualTo("螺纹钢2210");
		assertThat(CtpContractNameResolver.getCNSymbolName("rb0000")).isEqualTo("螺纹钢0000");
		assertThat(CtpContractNameResolver.getCNSymbolName("rb")).isEqualTo("螺纹钢");
		
		assertThat(CtpContractNameResolver.getCNSymbolName("i2306-C-680")).isEqualTo("铁矿石2306买权680");
		assertThat(CtpContractNameResolver.getCNSymbolName("TA306C4700")).isEqualTo("PTA306买权4700");
		assertThat(CtpContractNameResolver.getCNSymbolName("si2310-P-13000")).isEqualTo("工业硅2310卖权13000");
		assertThat(CtpContractNameResolver.getCNSymbolName("MA309P2225")).isEqualTo("甲醇309卖权2225");
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
