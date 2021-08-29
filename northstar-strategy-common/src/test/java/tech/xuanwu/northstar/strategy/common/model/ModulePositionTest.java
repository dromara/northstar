package tech.xuanwu.northstar.strategy.common.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import tech.xuanwu.northstar.strategy.common.TestFieldFactory;
import tech.xuanwu.northstar.strategy.common.model.entity.ModulePositionEntity;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

public class ModulePositionTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	private final String SYMBOL = "rb2210@SHFE@FUTURES";
	
	private TestFieldFactory factory = new TestFieldFactory("testGateway");
	
	private ModulePositionEntity.ModulePositionEntityBuilder proto = ModulePositionEntity.builder()
			.unifiedSymbol(SYMBOL)
			.multiplier(10)
			.volume(2)
			.openPrice(1234)
			.positionDir(PositionDirectionEnum.PD_Long);

	@Test
	public void testOnUpdate() {
		ModulePosition pos = new ModulePosition(proto.build());
		assertThat(pos.onUpdate(factory.makeTickField("rb2210", 2000))).isEqualTo(15320);
		
		ModulePosition pos2 = new ModulePosition(proto.positionDir(PositionDirectionEnum.PD_Short).build());
		assertThat(pos2.onUpdate(factory.makeTickField("rb2210", 2000))).isEqualTo(-15320);
	}
	
	@Test
	public void shouldNotTriggerStopLoss() {
		ModulePosition pos = new ModulePosition(proto.build());
		assertThat(pos.triggerStopLoss(factory.makeTickField("rb2210", Integer.MIN_VALUE))).isFalse();
		
		ModulePosition pos2 = new ModulePosition(proto.positionDir(PositionDirectionEnum.PD_Short).build());
		assertThat(pos2.triggerStopLoss(factory.makeTickField("rb2210", Integer.MAX_VALUE))).isFalse();
		
		ModulePosition pos3 = new ModulePosition(proto.positionDir(PositionDirectionEnum.PD_Short).stopLossPrice(1500).build());
		assertThat(pos3.triggerStopLoss(factory.makeTickField("rb2210", 1499))).isFalse();
		
		ModulePosition pos4 = new ModulePosition(proto.positionDir(PositionDirectionEnum.PD_Long).stopLossPrice(1000).build());
		assertThat(pos4.triggerStopLoss(factory.makeTickField("rb2210", 1001))).isFalse();
	}
	
	
	@Test
	public void shouldTriggerStopLoss() {
		ModulePosition pos = new ModulePosition(proto.stopLossPrice(1000).build());
		assertThat(pos.triggerStopLoss(factory.makeTickField("rb2210", 1000))).isTrue();
		
		ModulePosition pos2 = new ModulePosition(proto.positionDir(PositionDirectionEnum.PD_Short).stopLossPrice(1300).build());
		assertThat(pos2.triggerStopLoss(factory.makeTickField("rb2210", 1300))).isTrue();
	}
	
	
	@Test(expected = IllegalStateException.class)
	public void shouldThrowExceptionWhenMatchFail() {
		ModulePosition pos = new ModulePosition(proto.build());
		pos.onUpdate(factory.makeTickField("rb2109", 2000));
	}
	
	@Test
	public void shouldGetTheSameObject() {
		ModulePositionEntity e = proto.build();
		ModulePosition pos = new ModulePosition(e);
		assertThat(pos.convertToEntity()).isEqualTo(e);
	}
	
	@Test
	public void shouldGetEmptyState() {
		ModulePosition pos = new ModulePosition(proto.volume(0).build());
		assertThat(pos.isEmpty()).isTrue();
	}
	
	@Test
	public void shouldGetNonEmptyState() {
		ModulePosition pos = new ModulePosition(proto.build());
		assertThat(pos.isEmpty()).isFalse();
	}
	
	@Test
	public void shouldIncreasePosition() {
		ModulePosition pos = new ModulePosition(proto.build());
		assertThat(pos.onTrade(factory.makeTradeField("rb2210", 1311, 3, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open))).isTrue();
		
		ModulePosition pos2 = new ModulePosition(proto.positionDir(PositionDirectionEnum.PD_Short).build());
		assertThat(pos2.onTrade(factory.makeTradeField("rb2210", 1311, 3, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Open))).isTrue();
	}
	
	@Test
	public void shouldDecreasePosition() {
		ModulePosition pos = new ModulePosition(proto.build());
		assertThat(pos.onTrade(factory.makeTradeField("rb2210", 1311, 2, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Close))).isTrue();
		
		ModulePosition pos2 = new ModulePosition(proto.positionDir(PositionDirectionEnum.PD_Short).build());
		assertThat(pos2.onTrade(factory.makeTradeField("rb2210", 1311, 2, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Close))).isTrue();
	}
	
	@Test
	public void shouldHaveNoEffectOnPosition() {
		ModulePosition pos = new ModulePosition(proto.build());
		assertThat(pos.onTrade(factory.makeTradeField("rb2210", 1311, 2, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Close))).isFalse();
		assertThat(pos.onTrade(factory.makeTradeField("rb2210", 1311, 2, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Open))).isFalse();
	}
	
	@Test(expected = IllegalStateException.class)
	public void shouldGetException() {
		ModulePosition pos = new ModulePosition(proto.build());
		assertThat(pos.onTrade(factory.makeTradeField("rb2210", 1311, 3, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Close))).isTrue();
	}
	
}
