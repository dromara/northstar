package xyz.redtorch.gateway.ctp.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import tech.quantit.northstar.gateway.api.domain.NormalContract;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;

class CtpSubscriptionManagerTest {
	
	static {
		System.setProperty("subscription.ctp.classType.whitelist", "FUTURES");
		System.setProperty("subscription.ctp.classType.blacklist", "OPTION");
		System.setProperty("subscription.ctp.unifiedSymbol.whitelist", "[A-z]+\\d+@[A-Z]+@FUTURES");
		System.setProperty("subscription.ctp.unifiedSymbol.blacklist", "2210");
	}

	@Test
	void testWhitelist() {
		CtpSubscriptionManager subMgr = new CtpSubscriptionManager();
		NormalContract contract1 = mock(NormalContract.class);
		NormalContract contract2 = mock(NormalContract.class);
		NormalContract contract3 = mock(NormalContract.class);
		NormalContract contract4 = mock(NormalContract.class);
		when(contract1.unifiedSymbol()).thenReturn("rb2210@SHFE@FUTURES");
		when(contract2.unifiedSymbol()).thenReturn("rb2211@SHFE@FUTURES");
		when(contract3.unifiedSymbol()).thenReturn("rb2210-4000@SHFE@OPTION");
		when(contract4.unifiedSymbol()).thenReturn("rb2211-4000@SHFE@OPTION");
		when(contract1.productClass()).thenReturn(ProductClassEnum.FUTURES);
		when(contract2.productClass()).thenReturn(ProductClassEnum.FUTURES);
		when(contract3.productClass()).thenReturn(ProductClassEnum.OPTION);
		when(contract4.productClass()).thenReturn(ProductClassEnum.OPTION);
		
		assertThat(subMgr.subscribable(contract1)).isFalse();
		assertThat(subMgr.subscribable(contract2)).isTrue();
		assertThat(subMgr.subscribable(contract3)).isFalse();
		assertThat(subMgr.subscribable(contract4)).isFalse();
	}

}
