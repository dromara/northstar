package tech.quantit.northstar.domain.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.quantit.northstar.strategy.api.constant.ModuleState;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreField.ContractField;

public class ModuleStatusTest {
	
	TestFieldFactory factory = new TestFieldFactory("gateway");
	ContractField contract = factory.makeContract("rb2201");
	
	ModulePosition p1, p2, p3, p4;
	
	@BeforeEach
	public void prepare() {
		p1 = mock(ModulePosition.class);
		p2 = mock(ModulePosition.class);
		p3 = mock(ModulePosition.class);
		p4 = mock(ModulePosition.class);
		when(p1.getProfit()).thenReturn(200D);
		when(p2.getProfit()).thenReturn(155D);
		when(p3.getProfit()).thenReturn(15D);
		when(p4.getProfit()).thenReturn(15D);
		
		when(p1.getDirection()).thenReturn(PositionDirectionEnum.PD_Long);
		when(p2.getDirection()).thenReturn(PositionDirectionEnum.PD_Short);
		when(p3.getDirection()).thenReturn(PositionDirectionEnum.PD_Short);
		when(p4.getDirection()).thenReturn(PositionDirectionEnum.PD_Long);
		
		when(p1.getVolume()).thenReturn(2);
		when(p2.getVolume()).thenReturn(1);
		when(p3.getVolume()).thenReturn(2);
		when(p4.getVolume()).thenReturn(1);
		
		when(p1.contract()).thenReturn(contract);
		when(p2.contract()).thenReturn(contract);
		when(p3.contract()).thenReturn(contract);
		when(p4.contract()).thenReturn(contract);
	}
	
	@Test
	public void initStateShouldBeEmpty() {
		ModuleStatus ms = new ModuleStatus("test");
		assertThat(ms.at(ModuleState.EMPTY)).isTrue();
	}

	@Test
	public void shouldGetLongWhenupdatePosition() {
		ModuleStatus ms = new ModuleStatus("test");
		ms.updatePosition(p1);
		assertThat(ms.at(ModuleState.HOLDING_LONG)).isTrue();
	}
	
	@Test
	public void shouldGetShortWhenupdatePosition() {
		ModuleStatus ms = new ModuleStatus("test");
		ms.updatePosition(p2);
		assertThat(ms.at(ModuleState.HOLDING_SHORT)).isTrue();
	}
	
	@Test
	public void shouldGetEmptyWhenRemovePosition() {
		ModuleStatus ms = new ModuleStatus("test");
		ms.updatePosition(p1);
		ms.removePosition(p1.contract().getUnifiedSymbol(), p1.getDirection());
		ms.updatePosition(p2);
		ms.removePosition(p2.contract().getUnifiedSymbol(), p2.getDirection());
		assertThat(ms.at(ModuleState.EMPTY)).isTrue();
	}
	
	@Test
	public void shouldGetLongWhenHedging() {
		ModuleStatus ms = new ModuleStatus("test");
		ms.updatePosition(p1);
		ms.updatePosition(p2);
		assertThat(ms.at(ModuleState.HOLDING_LONG)).isTrue();
	}
	
	@Test
	public void shouldGetShortWhenHedging() {
		ModuleStatus ms = new ModuleStatus("test");
		ms.updatePosition(p3);
		ms.updatePosition(p4);
		assertThat(ms.at(ModuleState.HOLDING_SHORT)).isTrue();
	}
	
}
