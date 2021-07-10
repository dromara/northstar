package tech.xuanwu.northstar.strategy.common.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.Test;

import tech.xuanwu.northstar.gateway.api.TradeGateway;
import tech.xuanwu.northstar.strategy.common.constants.ModuleState;
import tech.xuanwu.northstar.strategy.common.event.ModuleEvent;
import tech.xuanwu.northstar.strategy.common.event.ModuleEventType;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TradeField;

public class ModuleAgentTest {

	private ModuleAgent agent = new ModuleAgent("testModule", null, ModuleState.EMPTY, mock(TradeGateway.class), "testGateway", true, "20210711");
	
	private ContractField contract = ContractField.newBuilder()
			.setUnifiedSymbol("rb2110@SHFE@FUTURES")
			.setSymbol("rb2110")
			.setLongMarginRatio(0.08)
			.setShortMarginRatio(0.08)
			.setPriceTick(10)
			.setMultiplier(10)
			.build();

	private TradeField openTrade = TradeField.newBuilder()
			.setOriginOrderId("123456")
			.setDirection(DirectionEnum.D_Buy)
			.setOffsetFlag(OffsetFlagEnum.OF_Open)
			.setContract(contract)
			.setTradeTimestamp(LocalDateTime.of(2021, 6, 30, 21, 01).toEpochSecond(ZoneOffset.ofHours(8)) * 1000)
			.setPrice(1234)
			.setVolume(1)
			.build();
	
	private TradeField closeTrade = TradeField.newBuilder()
			.setOriginOrderId("987654")
			.setDirection(DirectionEnum.D_Sell)
			.setOffsetFlag(OffsetFlagEnum.OF_Close)
			.setContract(contract)
			.setPrice(1204)
			.setVolume(2)
			.build();
	
	@Test
	public void testUpdateTradingDay() {
		assertThat(agent.getTradingDay()).isEqualTo("20210711");
		agent.updateTradingDay("20210713");
		assertThat(agent.getTradingDay()).isEqualTo("20210713");
	}

	@Test
	public void testHasOrderRecord() {
		agent.onEvent(ModuleEvent.builder()
				.eventType(ModuleEventType.ORDER_REQ_ACCEPTED)
				.data(SubmitOrderReqField.newBuilder()
						.setOffsetFlag(OffsetFlagEnum.OF_Open)
						.setOriginOrderId("123456").build())
				.build());
		assertThat(agent.hasOrderRecord("123456")).isTrue();
		agent.onOrder(OrderField.newBuilder()
				.setOriginOrderId("123456")
				.setOrderId("654321")
				.build());
		assertThat(agent.hasOrderRecord("654321")).isTrue();
		
		agent.onOrder(OrderField.newBuilder()
				.setOriginOrderId("00789456")
				.setOrderId("987654")
				.build());
		assertThat(agent.hasOrderRecord("00789456")).isFalse();
		
	}

	@Test
	public void testNumOfOpeningForToday() {
		agent.onEvent(ModuleEvent.builder()
				.eventType(ModuleEventType.ORDER_REQ_ACCEPTED)
				.data(SubmitOrderReqField.newBuilder()
						.setOffsetFlag(OffsetFlagEnum.OF_Open)
						.setOriginOrderId("123456").build())
				.build());
		agent.onOrder(OrderField.newBuilder()
				.setOrderId("654987")
				.setOriginOrderId("123456")
				.build());
		agent.onTrade(openTrade);
		assertThat(agent.numOfOpeningForToday()).isEqualTo(1);
	}

	@Test
	public void testModuleState() {
		assertThat(agent.getModuleState()).isEqualTo(ModuleState.EMPTY);
		agent.onEvent(ModuleEvent.builder()
				.eventType(ModuleEventType.ORDER_REQ_ACCEPTED)
				.data(SubmitOrderReqField.newBuilder()
						.setOffsetFlag(OffsetFlagEnum.OF_Open)
						.setOriginOrderId("123456").build())
				.build());
		assertThat(agent.getModuleState()).isEqualTo(ModuleState.PLACING_ORDER);
		agent.onOrder(OrderField.newBuilder()
				.setOriginOrderId("123456")
				.setOrderId("654321")
				.build());
		assertThat(agent.getModuleState()).isEqualTo(ModuleState.PENDING_ORDER);
		
		agent.onTrade(openTrade);
		assertThat(agent.getModuleState()).isEqualTo(ModuleState.HOLDING);
		
		agent.onEvent(ModuleEvent.builder()
				.eventType(ModuleEventType.ORDER_REQ_ACCEPTED)
				.data(SubmitOrderReqField.newBuilder()
						.setOffsetFlag(OffsetFlagEnum.OF_Close)
						.setOriginOrderId("987654").build())
				.build());
		assertThat(agent.getModuleState()).isEqualTo(ModuleState.PLACING_ORDER);
		agent.onOrder(OrderField.newBuilder()
				.setOriginOrderId("987654")
				.setOrderId("654321")
				.build());
		assertThat(agent.getModuleState()).isEqualTo(ModuleState.PENDING_ORDER);
		agent.onTrade(closeTrade);
		assertThat(agent.getModuleState()).isEqualTo(ModuleState.EMPTY);
	}

}
