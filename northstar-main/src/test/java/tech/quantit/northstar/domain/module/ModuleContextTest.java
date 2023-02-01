package tech.quantit.northstar.domain.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.alibaba.fastjson.JSONObject;

import tech.quantit.northstar.common.constant.Constants;
import tech.quantit.northstar.common.constant.ModuleState;
import tech.quantit.northstar.common.constant.SignalOperation;
import tech.quantit.northstar.common.exception.TradeException;
import tech.quantit.northstar.common.model.ModuleAccountRuntimeDescription;
import tech.quantit.northstar.common.model.ModuleRuntimeDescription;
import tech.quantit.northstar.gateway.api.IContractManager;
import tech.quantit.northstar.gateway.api.TradeGateway;
import tech.quantit.northstar.gateway.api.domain.contract.Contract;
import tech.quantit.northstar.gateway.api.domain.time.GenericTradeTime;
import tech.quantit.northstar.strategy.api.ClosingStrategy;
import tech.quantit.northstar.strategy.api.IModule;
import tech.quantit.northstar.strategy.api.IModuleAccountStore;
import tech.quantit.northstar.strategy.api.TradeStrategy;
import tech.quantit.northstar.strategy.api.constant.PriceType;
import tech.quantit.northstar.strategy.api.indicator.TimeSeriesUnaryOperator;
import tech.quantit.northstar.strategy.api.indicator.Indicator.Configuration;
import tech.quantit.northstar.strategy.api.indicator.Indicator.ValueType;
import tech.quantit.northstar.strategy.api.indicator.function.AverageFunctions;
import tech.quantit.northstar.strategy.api.utils.trade.DealCollector;
import tech.quantit.northstar.strategy.api.utils.trade.TradeIntent;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

class ModuleContextTest {
	
	private TestFieldFactory factory = new TestFieldFactory("testGateway");
	
	private ContractField contract = factory.makeContract("rb2205");
	
	private TickField tick = factory.makeTickField("rb2205", 5010);
	
	private ModuleContext ctx;
	
	private final static String NAME = "testModule";
	
	@SuppressWarnings("unchecked")
	@BeforeEach
	void prepare() {
		TradeStrategy strategy = mock(TradeStrategy.class);
		TradeGateway gateway = mock(TradeGateway.class);
		IContractManager contractMgr = mock(IContractManager.class);
		IModule module = mock(IModule.class);
		Contract c = mock(Contract.class);
		when(c.contractField()).thenReturn(contract);
		when(c.tradeTimeDefinition()).thenReturn(new GenericTradeTime());
		when(module.isEnabled()).thenReturn(Boolean.TRUE);
		when(gateway.gatewayId()).thenReturn(contract.getGatewayId());
		
		ClosingStrategy closingStrategy = new FirstInFirstOutClosingStrategy();
		Map<String, ModuleAccountRuntimeDescription> accRtsMap = new HashMap<>();
		accRtsMap.put("testAccout", ModuleAccountRuntimeDescription.builder()
				.initBalance(10000)
				.accountId("testGateway")
				.build());
		ModuleRuntimeDescription mrd = ModuleRuntimeDescription.builder()
				.moduleName(NAME)
				.enabled(true)
				.moduleState(ModuleState.EMPTY)
				.accountRuntimeDescriptionMap(accRtsMap)
				.dataState(new JSONObject())
				.build();
		IModuleAccountStore accStore = new ModuleAccountStore(NAME, closingStrategy.getClosingPolicy(), mrd, contractMgr);
		
		ctx = new ModuleContext(NAME, strategy, accStore, closingStrategy, 3, 100, mock(DealCollector.class), 
				mock(Consumer.class), mock(Consumer.class), mock(BiConsumer.class));
		ctx.setModule(module);
		ctx.bindGatewayContracts(gateway, List.of(c));
		ctx.onTrade(TradeField.newBuilder()
				.setOriginOrderId(Constants.MOCK_ORDER_ID)
				.setContract(contract)
				.setGatewayId(contract.getGatewayId())
				.setAccountId(contract.getGatewayId())
				.setPrice(5000)
				.setVolume(1)
				.setDirection(DirectionEnum.D_Buy)
				.setOffsetFlag(OffsetFlagEnum.OF_Open)
				.build());
	}

	@Test
	void testSubmitOrderReqWithException() {
		ctx.onTick(tick);
		assertThrows(TradeException.class, () -> {
			ctx.submitOrderReq(contract, SignalOperation.BUY_OPEN, PriceType.LIMIT_PRICE, 10, 5000);
		});
		
		TradeIntent ti = TradeIntent.builder()
				.contract(contract)
				.operation(SignalOperation.BUY_OPEN)
				.volume(10)
				.priceType(PriceType.ANY_PRICE)
				.build();
		assertThrows(TradeException.class, () -> {
			ctx.submitOrderReq(ti);
		});
	}
	
	@Test
	void testSubmitOrderReq() {
		ctx.onTick(tick);
		assertDoesNotThrow(() -> {
			ctx.submitOrderReq(contract, SignalOperation.BUY_OPEN, PriceType.LIMIT_PRICE, 1, 5000);
		});
	}
	
	@Test
	void testSubmitOrderReq2() {
		ctx.onTick(tick);
		TradeIntent ti = TradeIntent.builder()
				.contract(contract)
				.operation(SignalOperation.BUY_OPEN)
				.volume(1)
				.priceType(PriceType.ANY_PRICE)
				.build();
		ctx.submitOrderReq(ti);
		assertDoesNotThrow(() -> {
			ctx.onTick(tick);
		});
	}

	@Test
	void testHoldingNetProfit() {
		ctx.onTick(tick);
		assertThat(ctx.holdingNetProfit()).isEqualTo(100);
	}
	
	@Test
	void testGetRuntimeDescription() {
		assertDoesNotThrow(() -> {
			ctx.getRuntimeDescription(true);
		});
	}
	
	@Test
	void testAvailablePosition() {
		assertThat(ctx.availablePosition(DirectionEnum.D_Buy, contract.getUnifiedSymbol())).isEqualTo(1);
		assertThat(ctx.availablePosition(DirectionEnum.D_Sell, contract.getUnifiedSymbol())).isZero();
	}
	
	@Test
	void testAddIndicator() {
		assertDoesNotThrow(() -> {
			ctx.newIndicator(Configuration.builder().bindedContract(contract).indicatorName("STL").build(), AverageFunctions.SETTLE());
		});
		
		assertDoesNotThrow(() -> {
			ctx.newIndicator(Configuration.builder().bindedContract(contract).indicatorName("C").build(), TimeSeriesUnaryOperator.identity());
		});

		assertDoesNotThrow(() -> {
			ctx.newIndicator(Configuration.builder().bindedContract(contract).indicatorName("VOL").build(), ValueType.VOL, TimeSeriesUnaryOperator.identity());
		});
	}
}
