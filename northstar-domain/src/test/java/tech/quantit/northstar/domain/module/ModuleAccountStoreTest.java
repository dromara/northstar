package tech.quantit.northstar.domain.module;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.quantit.northstar.common.constant.ClosingPolicy;
import tech.quantit.northstar.common.constant.ModuleState;
import tech.quantit.northstar.common.model.ModuleAccountRuntimeDescription;
import tech.quantit.northstar.common.model.ModuleRuntimeDescription;
import tech.quantit.northstar.common.model.ModulePositionDescription;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
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
				.commissionPerDeal(10)
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
				.accountDescriptions(mamap)
				.build();
		mas = new ModuleAccountStore("testModule", ClosingPolicy.FIFO, md);
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
		assertThat(mas.getPreBalance("testAccount")).isEqualTo(104150);
	}

	@Test
	void testGetInitBalance() {
		assertThat(mas.getInitBalance("testAccount")).isEqualTo(100000);
	}

	@Test
	void testGetPreBalance() {
		assertThat(mas.getPreBalance("testAccount")).isEqualTo(100170);
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
