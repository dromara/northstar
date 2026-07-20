package org.dromara.northstar.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.model.ContractChartDescription;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.ListValue;
import org.dromara.northstar.common.model.ModuleChartDescription;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.TimeValues;
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
