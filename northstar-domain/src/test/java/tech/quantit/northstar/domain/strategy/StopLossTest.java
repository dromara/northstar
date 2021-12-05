package tech.quantit.northstar.domain.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

class StopLossTest {
	
	TestFieldFactory factory = new TestFieldFactory("test");

	@Test
	void testNonStopLoss() {
		StopLoss sl = new StopLoss(PositionDirectionEnum.PD_Long, 0);
		assertThat(sl.isTriggered(factory.makeTickField("rb2102", 0))).isFalse();
		assertThat(sl.isTriggered(factory.makeTickField("rb2102", Integer.MAX_VALUE))).isFalse();
		assertThat(sl.isTriggered(factory.makeTickField("rb2102", Integer.MIN_VALUE))).isFalse();
	}
	
	@Test
	void testLongStopLoss() {
		StopLoss sl = new StopLoss(PositionDirectionEnum.PD_Long, 1000);
		assertThat(sl.isTriggered(factory.makeTickField("rb2102", 1000))).isTrue();
		assertThat(sl.isTriggered(factory.makeTickField("rb2102", Integer.MAX_VALUE))).isFalse();
		assertThat(sl.isTriggered(factory.makeTickField("rb2102", Integer.MIN_VALUE))).isTrue();
	}
	
	@Test
	void testShortStopLoss() {
		StopLoss sl = new StopLoss(PositionDirectionEnum.PD_Short, 1000);
		assertThat(sl.isTriggered(factory.makeTickField("rb2102", 1000))).isTrue();
		assertThat(sl.isTriggered(factory.makeTickField("rb2102", Integer.MAX_VALUE))).isTrue();
		assertThat(sl.isTriggered(factory.makeTickField("rb2102", Integer.MIN_VALUE))).isFalse();
	}

}
