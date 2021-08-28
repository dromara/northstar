package tech.xuanwu.northstar.strategy.common.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import tech.xuanwu.northstar.common.model.ModulePositionEntity;
import tech.xuanwu.northstar.strategy.common.TestFieldFactory;
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

	@Test
	public void testOnUpdate() {
		ModulePositionEntity e = ModulePositionEntity.builder()
				.unifiedSymbol(SYMBOL)
				.multiplier(10)
				.volume(2)
				.openPrice(1234)
				.positionDir(PositionDirectionEnum.PD_Long)
				.build();
		ModulePosition pos = new ModulePosition(e);
		assertThat(pos.onUpdate(factory.makeTickField("rb2210", 2000))).isEqualTo(15320);
		
		ModulePositionEntity e2 = ModulePositionEntity.builder()
				.unifiedSymbol(SYMBOL)
				.multiplier(10)
				.volume(2)
				.openPrice(1234)
				.positionDir(PositionDirectionEnum.PD_Short)
				.build();
		ModulePosition pos2 = new ModulePosition(e2);
		assertThat(pos2.onUpdate(factory.makeTickField("rb2210", 2000))).isEqualTo(-15320);
	}
	
	@Test
	public void shouldNotTriggerStopLoss() {
		ModulePositionEntity e = ModulePositionEntity.builder()
				.unifiedSymbol(SYMBOL)
				.multiplier(10)
				.volume(2)
				.openPrice(1234)
				.positionDir(PositionDirectionEnum.PD_Long)
				.build();
		ModulePosition pos = new ModulePosition(e);
		assertThat(pos.triggerStopLoss(factory.makeTickField("rb2210", Integer.MIN_VALUE))).isFalse();
		
		ModulePositionEntity e2 = ModulePositionEntity.builder()
				.unifiedSymbol(SYMBOL)
				.multiplier(10)
				.volume(2)
				.openPrice(1234)
				.positionDir(PositionDirectionEnum.PD_Short)
				.build();
		ModulePosition pos2 = new ModulePosition(e2);
		assertThat(pos2.triggerStopLoss(factory.makeTickField("rb2210", Integer.MAX_VALUE))).isFalse();
		
		ModulePositionEntity e3 = ModulePositionEntity.builder()
				.unifiedSymbol(SYMBOL)
				.multiplier(10)
				.volume(2)
				.openPrice(1234)
				.stopLossPrice(1500)
				.positionDir(PositionDirectionEnum.PD_Short)
				.build();
		ModulePosition pos3 = new ModulePosition(e3);
		assertThat(pos3.triggerStopLoss(factory.makeTickField("rb2210", 1499))).isFalse();
		
		ModulePositionEntity e4 = ModulePositionEntity.builder()
				.unifiedSymbol(SYMBOL)
				.multiplier(10)
				.volume(2)
				.openPrice(1234)
				.stopLossPrice(1000)
				.positionDir(PositionDirectionEnum.PD_Long)
				.build();
		ModulePosition pos4 = new ModulePosition(e4);
		assertThat(pos4.triggerStopLoss(factory.makeTickField("rb2210", 1001))).isFalse();
	}
	
	
	@Test
	public void shouldTriggerStopLoss() {
		ModulePositionEntity e = ModulePositionEntity.builder()
				.unifiedSymbol(SYMBOL)
				.multiplier(10)
				.volume(2)
				.openPrice(1234)
				.stopLossPrice(1000)
				.positionDir(PositionDirectionEnum.PD_Long)
				.build();
		ModulePosition pos = new ModulePosition(e);
		assertThat(pos.triggerStopLoss(factory.makeTickField("rb2210", 1000))).isTrue();
		
		ModulePositionEntity e2 = ModulePositionEntity.builder()
				.unifiedSymbol(SYMBOL)
				.multiplier(10)
				.volume(2)
				.openPrice(1234)
				.stopLossPrice(1300)
				.positionDir(PositionDirectionEnum.PD_Short)
				.build();
		ModulePosition pos2 = new ModulePosition(e2);
		assertThat(pos2.triggerStopLoss(factory.makeTickField("rb2210", 1300))).isTrue();
	}
	
	
	@Test(expected = IllegalStateException.class)
	public void shouldThrowExceptionWhenMatchFail() {
		ModulePositionEntity e = ModulePositionEntity.builder()
				.multiplier(10)
				.volume(2)
				.openPrice(1234)
				.positionDir(PositionDirectionEnum.PD_Long)
				.build();
		ModulePosition pos = new ModulePosition(e);
		pos.onUpdate(factory.makeTickField("rb2210", 2000));
	}
	
	@Test
	public void shouldGetTheSameObject() {
		ModulePositionEntity e = ModulePositionEntity.builder()
				.unifiedSymbol(SYMBOL)
				.multiplier(10)
				.volume(2)
				.openPrice(1234)
				.stopLossPrice(1000)
				.positionDir(PositionDirectionEnum.PD_Long)
				.build();
		ModulePosition pos = new ModulePosition(e);
		assertThat(pos.convertToEntity()).isEqualTo(e);
	}
	

}
