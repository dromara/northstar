package org.dromara.northstar.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.dromara.northstar.common.constant.Constants;
import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.constant.SignalOperation;
import org.dromara.northstar.common.model.ModuleAccountRuntimeDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.gateway.Contract;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.gateway.TradeGateway;
import org.dromara.northstar.gateway.common.domain.time.GenericTradeTime;
import org.dromara.northstar.indicator.function.AverageFunctions;
import org.dromara.northstar.module.legacy.DealCollector;
import org.dromara.northstar.module.legacy.FirstInFirstOutClosingStrategy;
import org.dromara.northstar.module.legacy.ModuleAccountStore;
import org.dromara.northstar.module.legacy.ModulePlaybackContext;
import org.dromara.northstar.strategy.ClosingStrategy;
import org.dromara.northstar.strategy.IModule;
import org.dromara.northstar.strategy.IModuleAccountStore;
import org.dromara.northstar.strategy.TimeSeriesUnaryOperator;
import org.dromara.northstar.strategy.TradeStrategy;
import org.dromara.northstar.strategy.constant.PriceType;
import org.dromara.northstar.strategy.model.TradeIntent;
import org.dromara.northstar.strategy.model.Indicator.Configuration;
import org.dromara.northstar.strategy.model.Indicator.ValueType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.alibaba.fastjson.JSONObject;

import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

class ModulePlaybackContextTest {

private TestFieldFactory factory = new TestFieldFactory("回测账户");
	
	private ContractField contract = factory.makeContract("rb2205");
	
	private TickField tick = factory.makeTickField("rb2205", 5010);
	
	private ModulePlaybackContext ctx;
	
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
		accRtsMap.put("回测账户", ModuleAccountRuntimeDescription.builder()
				.initBalance(10000)
				.accountId("回测账户")
				.build());
		ModuleRuntimeDescription mrd = ModuleRuntimeDescription.builder()
				.moduleName(NAME)
				.enabled(true)
				.moduleState(ModuleState.EMPTY)
				.accountRuntimeDescriptionMap(accRtsMap)
				.dataState(new JSONObject())
				.build();
		IModuleAccountStore accStore = new ModuleAccountStore(NAME, closingStrategy.getClosingPolicy(), mrd, contractMgr);
		
		ctx = new ModulePlaybackContext(NAME, strategy, accStore, 3, 100, mock(DealCollector.class), mock(Consumer.class), mock(Consumer.class));
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
				.timeout(3000)
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
		assertThat(ctx.holdingNetProfit()).isEqualTo(0);
	}
	
	@Test
	void testGetRuntimeDescription() {
		assertDoesNotThrow(() -> {
			ctx.getRuntimeDescription(true);
		});
	}
	
	@Test
	void testAvailablePosition() {
		assertThat(ctx.availablePosition(DirectionEnum.D_Buy, contract.getUnifiedSymbol())).isZero();
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