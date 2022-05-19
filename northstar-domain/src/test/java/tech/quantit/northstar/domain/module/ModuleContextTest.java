package tech.quantit.northstar.domain.module;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.quantit.northstar.common.constant.SignalOperation;
import tech.quantit.northstar.common.exception.TradeException;
import tech.quantit.northstar.gateway.api.TradeGateway;
import tech.quantit.northstar.strategy.api.ClosingStrategy;
import tech.quantit.northstar.strategy.api.IModule;
import tech.quantit.northstar.strategy.api.IModuleAccountStore;
import tech.quantit.northstar.strategy.api.TradeStrategy;
import tech.quantit.northstar.strategy.api.constant.PriceType;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.GatewaySettingField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.TickField;

class ModuleContextTest {
	
	private TestFieldFactory factory = new TestFieldFactory("testAccount");
	
	private ContractField contract = factory.makeContract("rb2205");
	
	private TickField tick = factory.makeTickField("rb2205", 5000);
	
	TradeStrategy strategy;
	IModuleAccountStore accStore;
	ClosingStrategy closingStrategy;
	IModule module;
	
	private ModuleContext ctx;
	
	@BeforeEach
	void prepare() {
		TradeStrategy strategy = mock(TradeStrategy.class);
		IModuleAccountStore accStore = mock(IModuleAccountStore.class);
		ClosingStrategy closingStrategy = mock(ClosingStrategy.class);
		IModule module = mock(IModule.class);
		when(module.isEnabled()).thenReturn(Boolean.TRUE);
		when(closingStrategy.resolveOperation(any(SignalOperation.class), any(PositionField.class))).thenReturn(OffsetFlagEnum.OF_Open);
		when(closingStrategy.resolveOperation(any(SignalOperation.class), eq(null))).thenReturn(OffsetFlagEnum.OF_Open);
		
		ctx = new ModuleContext(strategy, accStore, closingStrategy, 3);
		ctx.setModule(module);
	}

	@Test
	void testSubmitOrderReqWithException() {
		TradeGateway gateway = mock(TradeGateway.class);
		when(gateway.getGatewaySetting()).thenReturn(GatewaySettingField.newBuilder().setGatewayId("testAccount").build());
		ctx.bindGatewayContracts(gateway, List.of(contract));
		ctx.onTick(tick);
		assertThrows(TradeException.class, () -> {			
			ctx.submitOrderReq(contract, SignalOperation.BUY_OPEN, PriceType.ANY_PRICE, 1, 5000);
		});
		
		assertThrows(TradeException.class, () -> {			
			ctx.submitOrderReq(contract, SignalOperation.BUY_OPEN, PriceType.ANY_PRICE, 1, 0);
		});
	}
	
	@Test
	void testSubmitOrderReq() {
		TradeGateway gateway = mock(TradeGateway.class);
		when(gateway.getGatewaySetting()).thenReturn(GatewaySettingField.newBuilder().setGatewayId("testAccount").build());
		ctx.bindGatewayContracts(gateway, List.of(contract));
		ctx.onTick(tick);
		when(ctx.accStore.getPreBalance(anyString())).thenReturn(10000000D);
		assertDoesNotThrow(() -> {
			ctx.submitOrderReq(contract, SignalOperation.BUY_OPEN, PriceType.ANY_PRICE, 1, 0);
		});
		
		verify(gateway, times(1)).submitOrder(any());
	}


}
