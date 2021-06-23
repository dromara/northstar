package tech.xuanwu.northstar.strategy.common.constants;


import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class SignalStateTest {

	@Test
	public void testIsOpen() {
		assertThat(SignalState.BuyClose.isOpen()).isFalse();
		assertThat(SignalState.SellClose.isOpen()).isFalse();
		assertThat(SignalState.BuyOpen.isOpen()).isTrue();
		assertThat(SignalState.SellOpen.isOpen()).isTrue();
		assertThat(SignalState.ReversingBuy.isOpen()).isTrue();
		assertThat(SignalState.ReversingSell.isOpen()).isTrue();
	}

	@Test
	public void testIsReverse() {
		assertThat(SignalState.BuyClose.isReverse()).isFalse();
		assertThat(SignalState.SellClose.isReverse()).isFalse();
		assertThat(SignalState.BuyOpen.isReverse()).isFalse();
		assertThat(SignalState.SellOpen.isReverse()).isFalse();
		assertThat(SignalState.ReversingBuy.isReverse()).isTrue();
		assertThat(SignalState.ReversingSell.isReverse()).isTrue();
	}

	@Test
	public void testIsBuy() {
		assertThat(SignalState.BuyClose.isBuy()).isTrue();
		assertThat(SignalState.SellClose.isBuy()).isFalse();
		assertThat(SignalState.BuyOpen.isBuy()).isTrue();
		assertThat(SignalState.SellOpen.isBuy()).isFalse();
		assertThat(SignalState.ReversingBuy.isBuy()).isTrue();
		assertThat(SignalState.ReversingSell.isBuy()).isFalse();
	}

}
