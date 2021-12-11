package tech.quantit.northstar.domain.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.quantit.northstar.strategy.api.constant.ModuleState;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreField.TradeField;

class ModuleStatusTest {
	
	ModuleStatus ms;
	ModulePosition mp = mock(ModulePosition.class);
	
	TestFieldFactory factory = new TestFieldFactory("test");
	
	@BeforeEach
	void setup() {
		mp = mock(ModulePosition.class);
		when(mp.getVolume()).thenReturn(2);
		when(mp.getDirection()).thenReturn(PositionDirectionEnum.PD_Long);
	}

	@Test
	void testUpdatePosition() {
		ms = new ModuleStatus("name", mp);
		ms.updatePosition(factory.makeTradeField("rb2210", 1000, 1, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open));
		verify(mp).merge(any(TradeField.class));
	}

	@Test
	void testRemovePosition() {
		ms = new ModuleStatus("name", mp);
		ms.removePosition();
		verify(mp).clearout();
	}

	@Test
	void testAt() {
		ms = new ModuleStatus("name", mp);
		assertThat(ms.at(ModuleState.HOLDING_LONG)).isTrue();
	}

	@Test
	void testHoldingProfit() {
		ms = new ModuleStatus("name", mp);
		ms.holdingProfit();
		verify(mp).profit();
	}

}
