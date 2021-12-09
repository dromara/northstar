package tech.quantit.northstar.domain.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.quantit.northstar.common.event.NorthstarEvent;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.gateway.api.TradeGateway;
import tech.quantit.northstar.strategy.api.DealerPolicy;
import tech.quantit.northstar.strategy.api.EventDrivenComponent;
import tech.quantit.northstar.strategy.api.SignalPolicy;
import tech.quantit.northstar.strategy.api.constant.ModuleState;
import tech.quantit.northstar.strategy.api.event.ModuleEvent;
import tech.quantit.northstar.strategy.api.event.ModuleEventBus;
import tech.quantit.northstar.strategy.api.event.ModuleEventType;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TradeField;

public class StrategyModuleTest {
	
	StrategyModule module;
	
	TradeGateway tradeGateway = mock(TradeGateway.class);
	
	TestFieldFactory factory = new TestFieldFactory("test");
	
	@BeforeEach
	public void prepare() {
		module = new StrategyModule("mktGateway", tradeGateway, new ModuleStatus("module"));
	}

	@Test
	public void testAddComponent() {
		module.meb = mock(ModuleEventBus.class);
		module.addComponent(mock(EventDrivenComponent.class));
		verify(module.meb).register(any());
		assertThat(module.components).hasSize(1);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testToggleRunningState() {
		Consumer<Boolean> callback = mock(Consumer.class);
		module.setRunningStateChangeListener(callback);
		module.toggleRunningState();
		verify(callback).accept(any());
	}
	
	@Test
	public void testGetName() {
		assertThat(module.getName()).isEqualTo("module");
	}

	@Test
	public void testOnEventNorthstarEventOfAny() {
		module.meb = mock(ModuleEventBus.class);
		module.onEvent(new NorthstarEvent(NorthstarEventType.ACCOUNT, AccountField.newBuilder().build()));
		verify(module.meb).post(any());
	}
	
	@Test
	public void testOnEventNorthstarEventOfOrderWhenCancelling() {
		ModuleStatus moduleStatus = new ModuleStatus("module");
		moduleStatus.stateMachine = new ModuleStateMachine("module", ModuleState.PENDING_ORDER);
		module = new StrategyModule("mktGateway", tradeGateway, moduleStatus);
		module.meb = mock(ModuleEventBus.class);
		module.ti = mock(ModuleTradeIntent.class);
		OrderField order = OrderField.newBuilder()
				.setOffsetFlag(OffsetFlagEnum.OF_Open)
				.setOrderStatus(OrderStatusEnum.OS_Canceled)
				.build();
		module.onEvent(new NorthstarEvent(NorthstarEventType.ORDER, order));
		verify(module.meb, times(2)).post(any());
		verify(module.meb).post(any(ModuleEvent.class));
	}
	
	@Test
	public void testOnEventNorthstarEventOfOrderWhenOrdering() {
		ModuleStatus moduleStatus = new ModuleStatus("module");
		moduleStatus.stateMachine = new ModuleStateMachine("module", ModuleState.PLACING_ORDER);
		module = new StrategyModule("mktGateway", tradeGateway, moduleStatus);
		module.meb = mock(ModuleEventBus.class);
		module.ti = mock(ModuleTradeIntent.class);
		OrderField order = OrderField.newBuilder()
				.setOffsetFlag(OffsetFlagEnum.OF_Open)
				.setOrderStatus(OrderStatusEnum.OS_Unknown)
				.build();
		module.onEvent(new NorthstarEvent(NorthstarEventType.ORDER, order));
		verify(module.meb, times(2)).post(any());
		verify(module.meb, times(1)).post(any(ModuleEvent.class));
	}
	
	@Test
	public void testOnEventNorthstarEventOfBuyTrade() {
		ModuleStatus moduleStatus = new ModuleStatus("module");
		moduleStatus.stateMachine = new ModuleStateMachine("module", ModuleState.PENDING_ORDER);
		module = new StrategyModule("mktGateway", tradeGateway, moduleStatus);
		module.meb = mock(ModuleEventBus.class);
		module.ti = mock(ModuleTradeIntent.class);
		module.onEvent(new NorthstarEvent(NorthstarEventType.TRADE, TradeField.newBuilder().setDirection(DirectionEnum.D_Sell).build()));
		verify(module.meb).post(any());
		verify(module.meb, times(0)).post(any(ModuleEvent.class));
	}
	
	@Test
	public void testOnEventNorthstarEventOfSellTrade() {
		ModuleStatus moduleStatus = new ModuleStatus("module");
		moduleStatus.stateMachine = new ModuleStateMachine("module", ModuleState.PENDING_ORDER);
		module = new StrategyModule("mktGateway", tradeGateway, moduleStatus);
		module.meb = mock(ModuleEventBus.class);
		module.ti = mock(ModuleTradeIntent.class);
		module.onEvent(new NorthstarEvent(NorthstarEventType.TRADE, TradeField.newBuilder().setDirection(DirectionEnum.D_Buy).build()));
		verify(module.meb).post(any());
		verify(module.meb, times(0)).post(any(ModuleEvent.class));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testOnEventModuleEventOfStopLoss() {
		ModuleStatus moduleStatus = new ModuleStatus("module");
		moduleStatus.updatePosition(factory.makeTradeField("test", 1000, 1, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Open));
		module = new StrategyModule("mktGateway", tradeGateway, moduleStatus);
		module.meb = mock(ModuleEventBus.class);
		module.ti = mock(ModuleTradeIntent.class);
		module.setSubmitOrderHandler(mock(Consumer.class));
		ModuleTradeIntent mti = mock(ModuleTradeIntent.class);
		when(mti.getSubmitOrderReq()).thenReturn(factory.makeOrderReq("test", DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open, 0, 0, 0));
		module.onEvent(new ModuleEvent<>(ModuleEventType.STOP_LOSS, mti));
		verify(module.submitOrderHandler).accept(any());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testOnEventModuleEventOfOrderConfirm() {
		ModuleStatus moduleStatus = new ModuleStatus("module");
		moduleStatus.stateMachine = new ModuleStateMachine("module", ModuleState.PENDING_ORDER);
		module = new StrategyModule("mktGateway", tradeGateway, moduleStatus);
		module.meb = mock(ModuleEventBus.class);
		module.ti = mock(ModuleTradeIntent.class);
		module.setSubmitOrderHandler(mock(Consumer.class));
		module.onEvent(new ModuleEvent<>(ModuleEventType.ORDER_REQ_ACCEPTED, factory.makeOrderReq("test", DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open, 0, 0, 0)));
		verify(module.submitOrderHandler).accept(any());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testOnEventModuleEventOfOrderReqCancel() {
		ModuleStatus moduleStatus = new ModuleStatus("module");
		moduleStatus.stateMachine = new ModuleStateMachine("module", ModuleState.PENDING_ORDER);
		module = new StrategyModule("mktGateway", tradeGateway, moduleStatus);
		module.meb = mock(ModuleEventBus.class);
		module.ti = mock(ModuleTradeIntent.class);
		module.setCancelOrderHandler(mock(Consumer.class));
		module.onEvent(new ModuleEvent<>(ModuleEventType.ORDER_REQ_CANCELLED, factory.makeCancelReq(factory.makeOrderReq("test", DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open, 0, 0, 0))));
		verify(module.cancelOrderHandler).accept(any());
	}

	@Test
	public void testSetEventBus() {
		assertThrows(UnsupportedOperationException.class, ()->{			
			module.setEventBus(mock(ModuleEventBus.class));
		});
	}
	
	@Test
	public void testBindedSymbols() {
		SignalPolicy signal = mock(SignalPolicy.class);
		DealerPolicy dealer = mock(DealerPolicy.class);
		when(signal.bindedContractSymbol()).thenReturn("rb2210");
		when(dealer.bindedContractSymbol()).thenReturn("rb2201");
		module.addComponent(dealer);
		module.addComponent(signal);
		assertThat(module.bindedContractUnifiedSymbols()).hasSize(2);
	}

	
}
