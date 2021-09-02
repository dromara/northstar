package tech.xuanwu.northstar.strategy.common.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import tech.xuanwu.northstar.common.model.ContractManager;
import tech.xuanwu.northstar.strategy.common.model.persistence.ModulePositionPO;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

public class ModulePositionTest {

	@Before
	public void setUp() throws Exception {
		contractMgr = mock(ContractManager.class);
		when(contractMgr.getContract("rb2210@SHFE@FUTURES")).thenReturn(factory.makeContract("rb2210"));
	}

	@After
	public void tearDown() throws Exception {
	}
	
	private final String SYMBOL = "rb2210@SHFE@FUTURES";
	
	private TestFieldFactory factory = new TestFieldFactory("testGateway");
	
	private ContractManager contractMgr;
	
	private ModulePositionPO.ModulePositionPOBuilder proto = ModulePositionPO.builder()
			.unifiedSymbol(SYMBOL)
			.multiplier(10)
			.volume(2)
			.openPrice(1234)
			.positionDir(PositionDirectionEnum.PD_Long);

	@Test
	public void testOnUpdate() {
		ModulePosition pos = new ModulePosition(proto.build(), contractMgr);
		assertThat(pos.updateProfit(factory.makeTickField("rb2210", 2000))).isEqualTo(15320);
		
		ModulePosition pos2 = new ModulePosition(proto.positionDir(PositionDirectionEnum.PD_Short).build(), contractMgr);
		assertThat(pos2.updateProfit(factory.makeTickField("rb2210", 2000))).isEqualTo(-15320);
	}
	
	@Test
	public void shouldNotTriggerStopLoss() {
		ModulePosition pos = new ModulePosition(proto.build(), contractMgr);
		assertThat(pos.triggerStopLoss(factory.makeTickField("rb2210", Integer.MIN_VALUE))).isEmpty();
		
		ModulePosition pos2 = new ModulePosition(proto.positionDir(PositionDirectionEnum.PD_Short).build(), contractMgr);
		assertThat(pos2.triggerStopLoss(factory.makeTickField("rb2210", Integer.MAX_VALUE))).isEmpty();
		
		ModulePosition pos3 = new ModulePosition(proto.positionDir(PositionDirectionEnum.PD_Short).stopLossPrice(1500).build(), contractMgr);
		assertThat(pos3.triggerStopLoss(factory.makeTickField("rb2210", 1499))).isEmpty();
		
		ModulePosition pos4 = new ModulePosition(proto.positionDir(PositionDirectionEnum.PD_Long).stopLossPrice(1000).build(), contractMgr);
		assertThat(pos4.triggerStopLoss(factory.makeTickField("rb2210", 1001))).isEmpty();
	}
	
	
	@Test
	public void shouldTriggerStopLoss() {
		ModulePosition pos = new ModulePosition(proto.stopLossPrice(1000).build(), contractMgr);
		assertThat(pos.triggerStopLoss(factory.makeTickField("rb2210", 1000))).isPresent();
		
		ModulePosition pos2 = new ModulePosition(proto.positionDir(PositionDirectionEnum.PD_Short).stopLossPrice(1300).build(), contractMgr);
		assertThat(pos2.triggerStopLoss(factory.makeTickField("rb2210", 1300))).isPresent();
	}
	
	
	@Test(expected = IllegalStateException.class)
	public void shouldThrowExceptionWhenMatchFail() {
		ModulePosition pos = new ModulePosition(proto.build(), contractMgr);
		pos.updateProfit(factory.makeTickField("rb2109", 2000));
	}
	
	@Test
	public void shouldGetTheSameObject() {
		ModulePositionPO e = proto.build();
		ModulePosition pos = new ModulePosition(e, contractMgr);
		assertThat(pos.convertToEntity()).isEqualTo(e);
	}
	
	@Test
	public void shouldGetEmptyState() {
		ModulePosition pos = new ModulePosition(proto.volume(0).build(), contractMgr);
		assertThat(pos.isEmpty()).isTrue();
	}
	
	@Test
	public void shouldGetNonEmptyState() {
		ModulePosition pos = new ModulePosition(proto.build(), contractMgr);
		assertThat(pos.isEmpty()).isFalse();
	}
	
	@Test
	public void shouldIncreasePosition() {
		ModulePosition pos = new ModulePosition(proto.build(), contractMgr);
		assertThat(pos.onTrade(factory.makeTradeField("rb2210", 1311, 3, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open))).isTrue();
		
		ModulePosition pos2 = new ModulePosition(proto.positionDir(PositionDirectionEnum.PD_Short).build(), contractMgr);
		assertThat(pos2.onTrade(factory.makeTradeField("rb2210", 1311, 3, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Open))).isTrue();
	}
	
	@Test
	public void shouldDecreasePosition() {
		ModulePosition pos = new ModulePosition(proto.build(), contractMgr);
		assertThat(pos.onTrade(factory.makeTradeField("rb2210", 1311, 2, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Close))).isTrue();
		
		ModulePosition pos2 = new ModulePosition(proto.positionDir(PositionDirectionEnum.PD_Short).build(), contractMgr);
		assertThat(pos2.onTrade(factory.makeTradeField("rb2210", 1311, 2, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Close))).isTrue();
	}
	
	@Test
	public void shouldHaveNoEffectOnPosition() {
		ModulePosition pos = new ModulePosition(proto.build(), contractMgr);
		assertThat(pos.onTrade(factory.makeTradeField("rb2210", 1311, 2, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Close))).isFalse();
		assertThat(pos.onTrade(factory.makeTradeField("rb2210", 1311, 2, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Open))).isFalse();
	}
	
	@Test(expected = IllegalStateException.class)
	public void shouldGetException() {
		ModulePosition pos = new ModulePosition(proto.build(), contractMgr);
		assertThat(pos.onTrade(factory.makeTradeField("rb2210", 1311, 3, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Close))).isTrue();
	}
	
	@Test
	public void shouldMatchPosition() {
		ModulePosition pos = new ModulePosition(proto.build(), contractMgr);
		assertThat(pos.isMatch(SYMBOL)).isTrue();
	}
	
	@Test
	public void shouldNotMatchPosition() {
		ModulePosition pos = new ModulePosition(proto.build(), contractMgr);
		assertThat(pos.isMatch(SYMBOL + "S")).isFalse();
	}
}
