package tech.quantit.northstar.strategy.api.indicator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import xyz.redtorch.pb.CoreField.BarField;

class CloseSeriesTest {

	
	@Test
	void test() {
		BarField b1 = BarField.newBuilder().setClosePrice(1100).setActionTimestamp(1).build();
		BarField b2 = BarField.newBuilder().setClosePrice(1105).setActionTimestamp(2).build();
		BarField b3 = BarField.newBuilder().setClosePrice(1108).setActionTimestamp(3).build();
		BarField b4 = BarField.newBuilder().setClosePrice(1109).setActionTimestamp(4).build();
		BarField b5 = BarField.newBuilder().setClosePrice(1101).setActionTimestamp(5).build();
		
		Indicator close = new CloseSeries(4);
		close.onBar(b1);
		close.onBar(b2);
		close.onBar(b3);
		close.onBar(b4);
		close.onBar(b5);
	
		assertThat(close.value(0)).isEqualTo(1101);
		assertThat(close.value(1)).isEqualTo(1109);
		assertThat(close.value(-1)).isEqualTo(1105);
		assertThat(close.highestVal().getValue()).isEqualTo(1109);
		assertThat(close.lowestVal().getValue()).isEqualTo(1101);
		
		assertThat(close.valueOn(3)).contains(1108D);
		assertThat(close.valueOn(10)).isEmpty();
	}

}
