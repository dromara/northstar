package tech.quantit.northstar.gateway.sim.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;

class SimPositionTest {
	
	TestFieldFactory factory = new TestFieldFactory("test");

	SimPosition pos = new SimPosition(factory.makeTradeField("rb2210", 2000, 8, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open));
	
	@Test
	void testOnTick() {
		pos.onTick(factory.makeTickField("rb2210", 2200));
		assertThat(pos.getLastPrice()).isEqualTo(2200);
	}

	@Test
	void testAvailableVol() {
		assertThat(pos.availableVol()).isEqualTo(8);
		
		CloseTradeRequest req = mock(CloseTradeRequest.class);
		when(req.frozenVol()).thenReturn(5);
		pos.setCloseReq(req);
		
		assertThat(pos.availableVol()).isEqualTo(3);
		
	}

	@Test
	void testFrozenMargin() {
		assertThat(pos.frozenMargin()).isCloseTo(12800, offset(1e-6));
	}

	@Test
	void testProfit() {
		pos.onTick(factory.makeTickField("rb2210", 2200));
		assertThat(pos.profit()).isCloseTo(16000, offset(1e-6));
	}

}
