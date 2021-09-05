package tech.xuanwu.northstar.strategy.common.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import tech.xuanwu.northstar.common.constant.DateTimeConstant;
import tech.xuanwu.northstar.common.model.ContractManager;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TradeField;

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
	
	private ModulePosition.ModulePositionBuilder proto = ModulePosition.builder()
			.unifiedSymbol(SYMBOL)
			.multiplier(10)
			.volume(2)
			.openPrice(1234)
			.positionDir(PositionDirectionEnum.PD_Long);

	@Test
	public void testOnUpdate() {
		ModulePosition pos = proto.build();
		assertThat(pos.updateProfit(factory.makeTickField("rb2210", 2000))).isEqualTo(15320);
		
		ModulePosition pos2 = proto.positionDir(PositionDirectionEnum.PD_Short).build();
		assertThat(pos2.updateProfit(factory.makeTickField("rb2210", 2000))).isEqualTo(-15320);
	}
	
	@Test
	public void shouldNotTriggerStopLoss() {
		ModulePosition pos = proto.build();
		assertThat(pos.triggerStopLoss(factory.makeTickField("rb2210", Integer.MIN_VALUE), factory.makeContract("rb2210"))).isEmpty();
		
		ModulePosition pos2 = proto.positionDir(PositionDirectionEnum.PD_Short).build();
		assertThat(pos2.triggerStopLoss(factory.makeTickField("rb2210", Integer.MAX_VALUE), factory.makeContract("rb2210"))).isEmpty();
		
		ModulePosition pos3 = proto.positionDir(PositionDirectionEnum.PD_Short).stopLossPrice(1500).build();
		assertThat(pos3.triggerStopLoss(factory.makeTickField("rb2210", 1499), factory.makeContract("rb2210"))).isEmpty();
		
		ModulePosition pos4 = proto.positionDir(PositionDirectionEnum.PD_Long).stopLossPrice(1000).build();
		assertThat(pos4.triggerStopLoss(factory.makeTickField("rb2210", 1001), factory.makeContract("rb2210"))).isEmpty();
	}
	
	
	@Test
	public void shouldTriggerStopLoss() {
		ModulePosition pos = proto.stopLossPrice(1000).build();
		assertThat(pos.triggerStopLoss(factory.makeTickField("rb2210", 1000), factory.makeContract("rb2210"))).isPresent();
		
		ModulePosition pos2 = proto.positionDir(PositionDirectionEnum.PD_Short).stopLossPrice(1300).build();
		assertThat(pos2.triggerStopLoss(factory.makeTickField("rb2210", 1300), factory.makeContract("rb2210"))).isPresent();
		
		ModulePosition pos3 = proto.openTradingDay(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER)).positionDir(PositionDirectionEnum.PD_Short).stopLossPrice(1300).build();
		assertThat(pos3.triggerStopLoss(factory.makeTickField("rb2210", 1300), factory.makeContract("rb2210"))).isPresent();
	}
	
	
	@Test(expected = IllegalStateException.class)
	public void shouldThrowExceptionWhenMatchFail() {
		ModulePosition pos = proto.build();
		pos.updateProfit(factory.makeTickField("rb2109", 2000));
	}
	
	@Test
	public void shouldGetEmptyState() {
		ModulePosition pos = proto.volume(0).build();
		assertThat(pos.isEmpty()).isTrue();
	}
	
	@Test
	public void shouldGetNonEmptyState() {
		ModulePosition pos = proto.build();
		assertThat(pos.isEmpty()).isFalse();
	}
	
	@Test
	public void shouldIncreasePosition() {
		ModulePosition pos = proto.build();
		assertThat(pos.onOpenTrade(factory.makeTradeField("rb2210", 1311, 3, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open))).isPresent();
		
		ModulePosition pos2 = proto.positionDir(PositionDirectionEnum.PD_Short).build();
		assertThat(pos2.onOpenTrade(factory.makeTradeField("rb2210", 1311, 3, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Open))).isPresent();
	}
	
	@Test
	public void shouldDecreasePosition() {
		ModulePosition pos = proto.build();
		assertThat(pos.onCloseTrade(factory.makeTradeField("rb2210", 1311, 2, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Close))).isPresent();
		
		ModulePosition pos2 = proto.positionDir(PositionDirectionEnum.PD_Short).build();
		assertThat(pos2.onCloseTrade(factory.makeTradeField("rb2210", 1311, 2, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Close))).isPresent();
	}
	
	@Test
	public void shouldHaveNoEffectOnPosition() {
		ModulePosition pos = proto.build();
		assertThat(pos.onCloseTrade(factory.makeTradeField("rb2210", 1311, 2, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Close))).isEmpty();
		assertThat(pos.onOpenTrade(factory.makeTradeField("rb2210", 1311, 2, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Open))).isEmpty();
	}
	
	@Test(expected = IllegalStateException.class)
	public void shouldGetException() {
		ModulePosition pos = proto.build();
		pos.onCloseTrade(factory.makeTradeField("rb2210", 1311, 3, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Close));
	}
	
	@Test
	public void shouldBeLongPosition() {
		ModulePosition pos = proto.build();
		assertThat(pos.isLongPosition()).isTrue();
	}
	
	@Test
	public void shouldNotBeLongPosition() {
		ModulePosition pos = proto.positionDir(PositionDirectionEnum.PD_Net).build();
		assertThat(pos.isLongPosition()).isFalse();
	}
	
	@Test
	public void shouldBeShortPosition() {
		ModulePosition pos = proto.positionDir(PositionDirectionEnum.PD_Short).build();
		assertThat(pos.isShortPosition()).isTrue();
	}
	
	@Test
	public void shouldNotBeShortPosition() {
		ModulePosition pos = proto.positionDir(PositionDirectionEnum.PD_Net).build();
		assertThat(pos.isShortPosition()).isFalse();
	}
	
	@Test
	public void shouldGetUnifiedSymbol() {
		ModulePosition pos = proto.build();
		assertThat(pos.getUnifiedSymbol()).isEqualTo(SYMBOL);
	}
	
	@Test
	public void shouldGetHoldingProfit() {
		ModulePosition pos = proto.build();
		assertThat(pos.updateProfit(factory.makeTickField("rb2210", 2000))).isEqualTo(15320);
		assertThat(pos.getHoldingProfit()).isEqualTo(15320);
	}
	
	@Test(expected = IllegalStateException.class)
	public void shouldFailIfWrongStateForClosingLong() {
		ModulePosition pos = proto.build();
		assertThat(pos.onCloseTrade(factory.makeTradeField("rb2210", 1311, 2, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Open))).isPresent();
	}
	
	@Test(expected = IllegalStateException.class)
	public void shouldFailIfWrongStateForClosingShort() {
		ModulePosition pos = proto.positionDir(PositionDirectionEnum.PD_Short).build();
		assertThat(pos.onCloseTrade(factory.makeTradeField("rb2210", 1311, 2, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Open))).isPresent();
	}
	
	@Test(expected = IllegalStateException.class)
	public void shouldFailIfWrongStateForOpeningLong() {
		ModulePosition pos = proto.build();
		assertThat(pos.onOpenTrade(factory.makeTradeField("rb2210", 1311, 2, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Close))).isPresent();
	}
	
	@Test(expected = IllegalStateException.class)
	public void shouldFailIfWrongStateForOpeningShort() {
		ModulePosition pos = proto.positionDir(PositionDirectionEnum.PD_Short).build();
		assertThat(pos.onOpenTrade(factory.makeTradeField("rb2210", 1311, 2, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Close))).isPresent();
	}
	
	@Test
	public void shouldCreatePosition() {
		TradeField trade = factory.makeTradeField("rb2210", 1234, 2, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open);
		OrderField order = OrderField.newBuilder()
				.setContract(trade.getContract())
				.setOriginOrderId(trade.getOriginOrderId())
				.setDirection(trade.getDirection())
				.setOffsetFlag(trade.getOffsetFlag())
				.setPrice(trade.getPrice())
				.build();
		assertThat(new ModulePosition(trade,  order)).isNotNull();
		
		TradeField trade2 = factory.makeTradeField("rb2210", 1234, 2, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Open);
		OrderField order2 = OrderField.newBuilder()
				.setContract(trade2.getContract())
				.setOriginOrderId(trade2.getOriginOrderId())
				.setDirection(trade2.getDirection())
				.setOffsetFlag(trade2.getOffsetFlag())
				.setPrice(trade2.getPrice())
				.build();
		assertThat(new ModulePosition(trade2,  order2)).isNotNull();
	}
	
	@Test(expected = IllegalStateException.class)
	public void shouldFailIfTradeAndOrderNotMatch() {
		TradeField trade = factory.makeTradeField("rb2210", 1234, 2, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open);
		OrderField order = OrderField.newBuilder()
				.setContract(trade.getContract())
				.setDirection(trade.getDirection())
				.setOffsetFlag(trade.getOffsetFlag())
				.setPrice(trade.getPrice())
				.build();
		new ModulePosition(trade,  order);
	}
	
	@Test
	public void shouldMatchPosition() {
		ModulePosition pos = proto.build();
		assertThat(pos.isMatch(SYMBOL)).isTrue();
	}
	
	@Test
	public void shouldNotMatchPosition() {
		ModulePosition pos = proto.build();
		assertThat(pos.isMatch(SYMBOL + "S")).isFalse();
	}
}
