package org.dromara.northstar.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dromara.northstar.common.constant.ClosingPolicy;
import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.ModuleAccountRuntimeDescription;
import org.dromara.northstar.common.model.ModulePositionDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.gateway.api.IContractManager;
import org.dromara.northstar.gateway.api.domain.contract.Contract;
import org.dromara.northstar.module.legacy.ModuleAccountStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

class ModuleAccountStoreTest {
	
	TestFieldFactory factory = new TestFieldFactory("testAccount");
	
	TradeField trade = factory.makeTradeField("rb2205", 1000, 2, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open);
	TradeField trade2 = factory.makeTradeField("rb2205", 1000, 2, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Open);
	TradeField closeTrade = factory.makeTradeField("rb2205", 1200, 2, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Close);
	
	TickField tick = factory.makeTickField("rb2205", 1000);
	
	ModuleAccountStore mas;
	
	@BeforeEach
	void prepare() {
		Map<String, ModuleAccountRuntimeDescription> mamap = new HashMap<>();
		ModuleAccountRuntimeDescription mad = ModuleAccountRuntimeDescription.builder()
				.accountId("testAccount")
				.initBalance(100000)
				.preBalance(100000)
				.accCloseProfit(200)
				.accCommission(10)
				.accDealVolume(3)
				.positionDescription(ModulePositionDescription.builder()
						.uncloseTrades(List.of(trade.toByteArray(), trade2.toByteArray()))
						.build())
				.build();
		mamap.put(mad.getAccountId(), mad);
		
		ModuleRuntimeDescription md = ModuleRuntimeDescription.builder()
				.moduleName("testModule")
				.enabled(true)
				.moduleState(ModuleState.HOLDING_LONG)
				.accountRuntimeDescriptionMap(mamap)
				.build();
		IContractManager contractMgr = mock(IContractManager.class);
		Contract c = mock(Contract.class);
		when(c.contractField()).thenReturn(ContractField.newBuilder().setCommissionRate(0.0001).build());
		when(contractMgr.getContract(any(Identifier.class))).thenReturn(c);
		mas = new ModuleAccountStore("testModule", ClosingPolicy.FIFO, md, contractMgr);
	}

	@Test
	void testGetModuleStateMachine() {
		assertThat(mas.getModuleState()).isEqualTo(ModuleState.EMPTY_HEDGE);
	}

	@Test
	void testOnTrade() {
		mas.onTrade(closeTrade);
		assertThat(mas.getModuleState()).isEqualTo(ModuleState.HOLDING_SHORT);
		assertThat(mas.getInitBalance("testAccount")).isEqualTo(100000);
		assertThat(mas.getAccCloseProfit("testAccount")).isEqualTo(4200);
		assertThat(mas.getAccDealVolume("testAccount")).isEqualTo(5);
		assertThat(mas.getUncloseTrades("testAccount")).hasSize(1);
		assertThat(mas.getPreBalance("testAccount")).isCloseTo(104185.2, offset(1e-4));
	}

	@Test
	void testGetInitBalance() {
		assertThat(mas.getInitBalance("testAccount")).isEqualTo(100000);
	}

	@Test
	void testGetPreBalance() {
		assertThat(mas.getPreBalance("testAccount")).isEqualTo(100190);
	}

	@Test
	void testGetUncloseTrades() {
		assertThat(mas.getUncloseTrades("testAccount")).hasSize(2);
	}

	@Test
	void testGetAccDealVolume() {
		assertThat(mas.getAccDealVolume("testAccount")).isEqualTo(3);
	}

	@Test
	void testGetAccCloseProfit() {
		assertThat(mas.getAccCloseProfit("testAccount")).isEqualTo(200);
	}

	@Test
	void testGetPositions() {
		mas.onTick(tick);
		assertThat(mas.getPositions("testAccount")).hasSize(2);
	}

}
