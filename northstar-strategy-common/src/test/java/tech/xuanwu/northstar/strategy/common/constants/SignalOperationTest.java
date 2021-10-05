package tech.xuanwu.northstar.strategy.common.constants;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class SignalOperationTest {

	@Test
	public void testIsOpen() {
		assertThat(SignalOperation.BuyClose.isOpen()).isFalse();
		assertThat(SignalOperation.SellClose.isOpen()).isFalse();
		assertThat(SignalOperation.BuyOpen.isOpen()).isTrue();
		assertThat(SignalOperation.SellOpen.isOpen()).isTrue();
		assertThat(SignalOperation.None.isOpen()).isFalse();
	}

	@Test
	public void testIsBuy() {
		assertThat(SignalOperation.BuyClose.isBuy()).isTrue();
		assertThat(SignalOperation.SellClose.isBuy()).isFalse();
		assertThat(SignalOperation.BuyOpen.isBuy()).isTrue();
		assertThat(SignalOperation.SellOpen.isBuy()).isFalse();
		assertThat(SignalOperation.None.isOpen()).isFalse();
	}
	
	@Test
	public void testIsSell() {
		assertThat(SignalOperation.BuyClose.isSell()).isFalse();
		assertThat(SignalOperation.SellClose.isSell()).isTrue();
		assertThat(SignalOperation.BuyOpen.isSell()).isFalse();
		assertThat(SignalOperation.SellOpen.isSell()).isTrue();
		assertThat(SignalOperation.None.isSell()).isFalse();
	}
	
	@Test
	public void testIsClose() {
		assertThat(SignalOperation.BuyClose.isClose()).isTrue();
		assertThat(SignalOperation.SellClose.isClose()).isTrue();
		assertThat(SignalOperation.BuyOpen.isClose()).isFalse();
		assertThat(SignalOperation.SellOpen.isClose()).isFalse();
		assertThat(SignalOperation.None.isClose()).isFalse();
	}

}
