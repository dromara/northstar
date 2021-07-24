package tech.xuanwu.northstar.strategy.common.constants;


import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class SignalStateTest {

	@Test
	public void testIsOpen() {
		assertThat(SignalOperation.BuyClose.isOpen()).isFalse();
		assertThat(SignalOperation.SellClose.isOpen()).isFalse();
		assertThat(SignalOperation.BuyOpen.isOpen()).isTrue();
		assertThat(SignalOperation.SellOpen.isOpen()).isTrue();
		assertThat(SignalOperation.ReversingBuy.isOpen()).isTrue();
		assertThat(SignalOperation.ReversingSell.isOpen()).isTrue();
	}

	@Test
	public void testIsReverse() {
		assertThat(SignalOperation.BuyClose.isReverse()).isFalse();
		assertThat(SignalOperation.SellClose.isReverse()).isFalse();
		assertThat(SignalOperation.BuyOpen.isReverse()).isFalse();
		assertThat(SignalOperation.SellOpen.isReverse()).isFalse();
		assertThat(SignalOperation.ReversingBuy.isReverse()).isTrue();
		assertThat(SignalOperation.ReversingSell.isReverse()).isTrue();
	}

	@Test
	public void testIsBuy() {
		assertThat(SignalOperation.BuyClose.isBuy()).isTrue();
		assertThat(SignalOperation.SellClose.isBuy()).isFalse();
		assertThat(SignalOperation.BuyOpen.isBuy()).isTrue();
		assertThat(SignalOperation.SellOpen.isBuy()).isFalse();
		assertThat(SignalOperation.ReversingBuy.isBuy()).isTrue();
		assertThat(SignalOperation.ReversingSell.isBuy()).isFalse();
	}

}
