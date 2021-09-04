package tech.xuanwu.northstar.strategy.common.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import tech.xuanwu.northstar.common.model.ContractManager;
import tech.xuanwu.northstar.strategy.common.constants.ModuleState;
import tech.xuanwu.northstar.strategy.common.event.ModuleEventType;
import tech.xuanwu.northstar.strategy.common.model.persistence.ModulePositionPO;
import tech.xuanwu.northstar.strategy.common.model.persistence.ModuleStatusPO;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TradeField;

public class ModuleStatusTest {
	
	private ContractManager contractMgr = mock(ContractManager.class);
	
	private ModuleStatus ms;
	
	private final String SYMBOL = "rb2210@SHFE@FUTURES";
	
	private final String SYMBOL2 = "rb2110@SHFE@FUTURES";
	
	private ModulePositionPO.ModulePositionPOBuilder proto = ModulePositionPO.builder()
			.unifiedSymbol(SYMBOL)
			.multiplier(10)
			.volume(2)
			.openPrice(1234)
			.stopLossPrice(1200)
			.positionDir(PositionDirectionEnum.PD_Long);
	
	private ModuleStatusPO mse;
	
	private TestFieldFactory factory = new TestFieldFactory("testGateway");

	@Before
	public void setUp() throws Exception {
		when(contractMgr.getContract(SYMBOL)).thenReturn(ContractField.newBuilder().build());
		mse = ModuleStatusPO.builder()
				.moduleName("testModuule")
				.state(ModuleState.HOLDING_LONG)
				.positions(Lists.newArrayList(proto.build(), proto.unifiedSymbol(SYMBOL2).build()))
				.holdingTradingDay("20210606")
				.countOfOpeningToday(3)
				.build();
		ms = new ModuleStatus(mse, contractMgr);
	}

	@After
	public void tearDown() throws Exception {
		
	}

	@Test
	public void shouldUpdateHoldingProfit() {
		assertThat(ms.updateHoldingProfit(factory.makeTickField("rb2210", 1240))).isEqualTo(120);
	}
	
	@Test
	public void shouldTriggerStopLoss() {
		assertThat(ms.triggerStopLoss(factory.makeTickField("rb2210", 1200))).isPresent();
	}
	
	@Test
	public void shouldNotTriggerStopLoss() {
		assertThat(ms.triggerStopLoss(factory.makeTickField("rb2210", 1201))).isNotPresent();
	}

	@Test
	public void shouldUpdateOpeningTrade() {
		TradeField trade = factory.makeTradeField("rb2210", 1240, 2, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open);
		ms.onTrade(trade, OrderField.newBuilder().setOriginOrderId(trade.getOriginOrderId()).build());
		assertThat(ms.longPositions).hasSize(2);
		assertThat(ms.longPositions.get(SYMBOL).volume).isEqualTo(4);
		
		TradeField trade2 = factory.makeTradeField("rb2210", 1240, 2, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Open);
		ms.onTrade(trade2, OrderField.newBuilder().setOriginOrderId(trade2.getOriginOrderId()).build());
		assertThat(ms.shortPositions).hasSize(1);
		
		TradeField trade3 = factory.makeTradeField("rb2201", 1240, 2, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open);
		ms.onTrade(trade3, OrderField.newBuilder().setOriginOrderId(trade3.getOriginOrderId()).build());
		assertThat(ms.longPositions).hasSize(3);
	}
	
	@Test
	public void shouldUpdateClosingTrade() {
		TradeField trade = factory.makeTradeField("rb2210", 1240, 2, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Close);
		ms.onTrade(trade, OrderField.newBuilder().setOriginOrderId(trade.getOriginOrderId()).build());
		assertThat(ms.longPositions).hasSize(1);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldFailIfOriginIdMismatch() {
		TradeField trade = factory.makeTradeField("rb2209", 1240, 2, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open);
		ms.onTrade(trade, OrderField.newBuilder().setContract(trade.getContract()).build());
	}
	
	@Test(expected = IllegalStateException.class)
	public void shouldFailIfTradeOffsetUnknow() {
		TradeField trade = factory.makeTradeField("rb2210", 1240, 2, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Unknown);
		ms.onTrade(trade, OrderField.newBuilder().setOriginOrderId(trade.getOriginOrderId()).build());
	}
	
	@Test
	public void shouldGetTheSameEntity() {
		mse.getPositions().sort((a,b)-> a.getUnifiedSymbol().compareTo(b.getUnifiedSymbol()));
		assertThat(ms.convertToEntity()).isEqualTo(mse);
	}
	
	@Test
	public void shouldGetState() {
		assertThat(ms.at(ModuleState.HOLDING_LONG)).isTrue();
		assertThat(ms.getCurrentState()).isEqualTo(ModuleState.HOLDING_LONG);
		assertThat(ms.at(ModuleState.EMPTY)).isFalse();
	}
	
	@Test
	public void shouldGetStateChange() {
		assertThat(ms.transform(ModuleEventType.CLOSING_SIGNAL_CREATED)).isEqualTo(ModuleState.PLACING_ORDER);
	}
	
	@Test
	public void shouldGetName() {
		assertThat(ms.getModuleName()).isEqualTo("testModuule");
	}
	
	@Test
	public void shouldBeTheSameTradingDay() {
		assertThat(ms.isSameDay("20210606")).isTrue();
	}
	
	@Test
	public void shouldNotBeTheSameTradingDay() {
		assertThat(ms.isSameDay("20210607")).isFalse();
	}
	
	@Test
	public void shouldBeAbleToGetAndSetAccountAvailable() {
		ms = new ModuleStatus("testModule", contractMgr);
		ms.setAccountAvailable(1000);
		assertThat(ms.getAccountAvailable()).isEqualTo(1000);
	}
	
	@Test
	public void shouldGetCountOfOpeningToday() {
		assertThat(ms.getCountOfOpeningToday()).isEqualTo(3);
	}
	
	@Test
	public void shouldGetHoldingProfit() {
		ms.updateHoldingProfit(factory.makeTickField("rb2210", 1240));
		assertThat(ms.getHoldingProfit()).isEqualTo(120);
	}
}
