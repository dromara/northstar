package tech.quantit.northstar.domain.module;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import tech.quantit.northstar.common.constant.ClosingPolicy;
import tech.quantit.northstar.common.constant.SignalOperation;
import tech.quantit.northstar.strategy.api.ClosingStrategy;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.PositionField;

class PriorTodayClosingStrategyTest {

	ClosingStrategy cs = new PriorTodayClosingStrategy();
	
	PositionField pf1 = PositionField.newBuilder()
			.setTdPosition(2)
			.setYdPosition(2)
			.build();
	
	PositionField pf2 = PositionField.newBuilder()
			.setTdPosition(2)
			.build();

	@Test
	void testResolveOperation() {
		assertThat(cs.resolveOperation(SignalOperation.BUY_CLOSE, pf1)).isEqualTo(OffsetFlagEnum.OF_CloseToday);
		assertThat(cs.resolveOperation(SignalOperation.BUY_CLOSE, pf2)).isEqualTo(OffsetFlagEnum.OF_CloseToday);
		assertThat(cs.resolveOperation(SignalOperation.BUY_OPEN, null)).isEqualTo(OffsetFlagEnum.OF_Open);
	}

	@Test
	void testGetClosingPolicy() {
		assertThat(cs.getClosingPolicy()).isEqualTo(ClosingPolicy.PRIOR_TODAY);
	}

}
