package org.dromara.northstar.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.model.BacktestModuleVO;
import org.dromara.northstar.common.model.ContractChartDescription;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.DealRecord;
import org.dromara.northstar.common.model.ListValue;
import org.dromara.northstar.common.model.ModuleChartDescription;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.TimeValues;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.data.IAccountRepository;
import org.dromara.northstar.strategy.IModule;
import org.dromara.northstar.strategy.IModuleContext;
import org.junit.jupiter.api.Test;

class BacktestRunnerTest {

	@Test
	void shouldReturnModuleChartDescriptionInTradeDetails() {
		ContractSimpleInfo contract = ContractSimpleInfo.builder()
				.id("ao0000@SHFE@FUTURES@PLAYBACK")
				.name("氧化铝指数")
				.build();
		ModuleChartDescription chartDescription = ModuleChartDescription.builder()
				.data(Map.of(Interval.M1, List.of(chart(contract, new String[] {"close"}, timeValues(1000, 100)))))
				.strategyInfos(List.of())
				.build();
		IModuleContext ctx = mock(IModuleContext.class);
		when(ctx.getModuleDescription()).thenReturn(ModuleDescription.builder().name("testModule").build());
		when(ctx.getChartDescription()).thenReturn(chartDescription);
		IModule module = mock(IModule.class);
		when(module.getModuleContext()).thenReturn(ctx);
		IAccountRepository accountRepo = mock(IAccountRepository.class);
		when(accountRepo.findDealRecords("testModule")).thenReturn(List.of());
		BacktestRunner runner = BacktestRunner.builder()
				.moduleList(List.of(module))
				.accountRepo(accountRepo)
				.build();

		var result = runner.getBacktestModuleTradeDetails("testModule");

		assertThat(result.getChartData().getData().get(Interval.M1)).hasSize(1);
		assertThat(result.getChartData().getData().get(Interval.M15)).isEmpty();
	}

	@Test
	void shouldReturnNullProfitLossRatioWhenNoLosingDeal() {
		// 全胜（无亏损）时盈亏比数学上为 +∞，统一以 null 表达（口径同 RLTrainingRunner.profitFactor）
		BacktestRunner runner = runnerWithDeals(List.of(
				DealRecord.builder().dealProfit(200).transactionFee(10).build(),
				DealRecord.builder().dealProfit(100).transactionFee(10).build()));

		List<BacktestModuleVO> result = runner.getBacktestModule();

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getProfitLossRatio()).isNull();
		assertThat(result.get(0).getWinRate()).isEqualTo(1.0);
		assertThat(result.get(0).getTotalTrades()).isEqualTo(2);
	}

	@Test
	void shouldReturnNullProfitLossRatioWhenNoDeal() {
		BacktestRunner runner = runnerWithDeals(List.of());

		BacktestModuleVO result = runner.getBacktestModule().get(0);

		assertThat(result.getProfitLossRatio()).isNull();
		assertThat(result.getWinRate()).isEqualTo(0);
	}

	@Test
	void shouldComputeProfitLossRatioWithLosses() {
		// 平均盈利 300 / 平均亏损 100 = 3.0
		BacktestRunner runner = runnerWithDeals(List.of(
				DealRecord.builder().dealProfit(300).transactionFee(10).build(),
				DealRecord.builder().dealProfit(-100).transactionFee(10).build()));

		BacktestModuleVO result = runner.getBacktestModule().get(0);

		assertThat(result.getProfitLossRatio()).isEqualTo(3.0);
		assertThat(result.getWinRate()).isEqualTo(0.5);
	}

	private BacktestRunner runnerWithDeals(List<DealRecord> deals) {
		Contract contract = Contract.builder()
				.contractId("rb0000@SHFE@FUTURES@PLAYBACK")
				.unifiedSymbol("rb0000@SHFE@FUTURES")
				.symbol("rb0000")
				.name("螺纹钢指数")
				.build();
		IModuleContext ctx = mock(IModuleContext.class);
		when(ctx.getModuleDescription()).thenReturn(ModuleDescription.builder()
				.name("testModule").initBalance(100_000).build());
		when(ctx.bindedContracts()).thenReturn(List.of(contract));
		when(ctx.getDefaultVolume()).thenReturn(1.0);
		ModuleAccount account = mock(ModuleAccount.class);
		when(account.getPositions()).thenReturn(List.of());
		when(ctx.getModuleAccount()).thenReturn(account);
		IModule module = mock(IModule.class);
		when(module.getModuleContext()).thenReturn(ctx);
		IAccountRepository accountRepo = mock(IAccountRepository.class);
		when(accountRepo.findDealRecords("testModule")).thenReturn(deals);
		return BacktestRunner.builder()
				.moduleList(List.of(module))
				.accountRepo(accountRepo)
				.fromDate(LocalDate.of(2024, 1, 1))
				.toDate(LocalDate.of(2024, 2, 1))
				.build();
	}

	@Test
	void shouldMergeChartSectionsByIntervalContractAndTimestamp() {
		ContractSimpleInfo contract = ContractSimpleInfo.builder()
				.id("ao0000@SHFE@FUTURES@PLAYBACK")
				.name("氧化铝指数")
				.symbol("ao0000")
				.build();
		ContractChartDescription firstSection = chart(contract, new String[] {"close", "EMA10"},
				timeValues(1000, 100, 99.5), timeValues(2000, 101, 100));
		ContractChartDescription secondSection = chart(contract, new String[] {"close", "EMA10"},
				timeValues(2000, 102, 100.5), timeValues(3000, 103, 101.5));
		List<ListValue> strategyInfos = List.of(ListValue.builder().name("info").build());

		ModuleChartDescription result = BacktestRunner.mergeChartDescriptions(List.of(
				ModuleChartDescription.builder()
						.data(Map.of(Interval.M1, List.of(firstSection)))
						.build(),
				ModuleChartDescription.builder()
						.data(Map.of(Interval.M1, List.of(secondSection)))
						.strategyInfos(strategyInfos)
						.build()));

		ContractChartDescription merged = result.getData().get(Interval.M1).getFirst();
		assertThat(merged.getFields()).containsExactly("close", "EMA10");
		assertThat(merged.getTimedValues()).extracting(TimeValues::getTimestamp)
				.containsExactly(1000L, 2000L, 3000L);
		assertThat(merged.getTimedValues().get(1).getValues()).containsExactly(102D, 100.5D);
		assertThat(result.getStrategyInfos()).isEqualTo(strategyInfos);
		assertThat(result.getData().get(Interval.M15)).isEmpty();
	}

	private ContractChartDescription chart(ContractSimpleInfo contract, String[] fields, TimeValues... values) {
		return ContractChartDescription.builder()
				.contract(contract)
				.fields(fields)
				.indicators(List.of())
				.timedValues(List.of(values))
				.build();
	}

	private TimeValues timeValues(long timestamp, double... values) {
		return TimeValues.builder()
				.timestamp(timestamp)
				.values(Arrays.stream(values).boxed().toArray(Double[]::new))
				.build();
	}
}
