package org.dromara.northstar.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.dromara.northstar.common.constant.IndicatorPlacement;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.model.ContractChartDescription;
import org.dromara.northstar.common.model.IndicatorVO;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.TimeSlot;
import org.dromara.northstar.common.model.core.TradeTimeDefinition;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.constant.ValueType;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.indicator.model.Num;
import org.dromara.northstar.strategy.IModuleContext;
import org.dromara.northstar.strategy.TradeStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class TimeFrameScopedRegistryTest {

	Contract contract = Contract.builder()
			.unifiedSymbol("rb2205@SHFE@FUTURES")
			.contractDefinition(ContractDefinition.builder()
					.tradeTimeDef(TradeTimeDefinition.builder()
							.timeSlots(List.of(TimeSlot.builder().start(LocalTime.of(9, 0)).end(LocalTime.of(15, 0)).build()))
							.build())
					.build())
			.build();

	IModuleContext context;
	TradeStrategy strategy;
	TimeFrameScopedRegistry registry;

	@BeforeEach
	void setup() {
		context = mock(IModuleContext.class);
		strategy = mock(TradeStrategy.class);
		when(context.getLogger(any())).thenReturn(LoggerFactory.getLogger(getClass()));
		when(context.bindedContracts()).thenReturn(List.of(contract));
		when(context.getModuleDescription()).thenReturn(ModuleDescription.builder().moduleCacheDataSize(10).build());
		when(context.getStrategy()).thenReturn(strategy);
		registry = new TimeFrameScopedRegistry(Interval.M1, context);
	}

	private Indicator mockIndicator(String name, boolean visible, Indicator... dependencies) {
		Indicator indicator = mock(Indicator.class);
		Configuration cfg = Configuration.builder()
				.indicatorName(name)
				.contract(contract)
				.interval(Interval.M1)
				.valueType(ValueType.CLOSE)
				.cacheLength(5)
				.visible(visible)
				.build();
		when(indicator.getConfiguration()).thenReturn(cfg);
		when(indicator.dependencies()).thenReturn(List.of(dependencies));
		return indicator;
	}

	private Bar barOf(long timestamp, boolean unstable) {
		return Bar.builder()
				.contract(contract)
				.interval(Interval.M1)
				.actionDay(LocalDate.of(2024, 1, 2))
				.actionTime(LocalTime.of(9, 1))
				.tradingDay(LocalDate.of(2024, 1, 2))
				.actionTimestamp(timestamp)
				.openPrice(1000)
				.highPrice(1010)
				.lowPrice(990)
				.closePrice(1005)
				.unstable(unstable)
				.build();
	}

	@Test
	void testRegisterIndicatorWithDependency_parentAndChildBothUpdated() {
		Indicator child = mockIndicator("child", false);
		Indicator parent = mockIndicator("parent", true, child);
		registry.registerIndicator(parent);

		registry.onBar(barOf(1000L, false));

		// 父指标与子指标都应该被更新（修复前父指标会被子指标的更新助手覆盖而收不到更新）
		verify(parent, times(1)).update(any(Num.class));
		verify(child, times(1)).update(any(Num.class));
	}

	@Test
	void testRegisterMultipleIndicatorsOnSameContract_allUpdated() {
		Indicator ind1 = mockIndicator("ind1", true);
		Indicator ind2 = mockIndicator("ind2", true);
		registry.registerIndicator(ind1);
		registry.registerIndicator(ind2);

		registry.onBar(barOf(1000L, false));

		// 同一合约的多个指标都应该被更新（修复前只有最后注册的指标能收到更新）
		verify(ind1, times(1)).update(any(Num.class));
		verify(ind2, times(1)).update(any(Num.class));
	}

	@Test
	void testSameIndicatorInstanceRegisteredTwice_isIdempotent() {
		Indicator ind = mockIndicator("ind", true);
		registry.registerIndicator(ind);
		registry.registerIndicator(ind);

		registry.onBar(barOf(1000L, false));

		verify(ind, times(1)).update(any(Num.class));
	}

	@Test
	void testSharedDependencyRegisteredOnce_updatedPerBar() {
		Indicator shared = mockIndicator("shared", false);
		Indicator parent1 = mockIndicator("parent1", true, shared);
		Indicator parent2 = mockIndicator("parent2", true, shared);
		registry.registerIndicator(parent1);
		registry.registerIndicator(parent2);

		registry.onBar(barOf(1000L, false));

		// 共享依赖会被两个父指标的助手各递归更新一次（更新本身是幂等设计）
		verify(parent1, times(1)).update(any(Num.class));
		verify(parent2, times(1)).update(any(Num.class));
		verify(shared, atLeastOnce()).update(any(Num.class));
	}

	@Test
	void testDuplicateVisibleIndicatorNameThrows() {
		registry.registerIndicator(mockIndicator("dup", true));
		assertThatThrownBy(() -> registry.registerIndicator(mockIndicator("dup", true)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("不能重名");
	}

	@Test
	void testInvisibleIndicatorAllowsDuplicateName() {
		registry.registerIndicator(mockIndicator("dup", false));
		assertThatCode(() -> registry.registerIndicator(mockIndicator("dup", false)))
				.doesNotThrowAnyException();
	}

	@Test
	void testNonPositiveCacheLengthThrows() {
		Indicator ind = mock(Indicator.class);
		Configuration cfg = Configuration.builder()
				.indicatorName("bad")
				.contract(contract)
				.interval(Interval.M1)
				.valueType(ValueType.CLOSE)
				.cacheLength(0)
				.visible(true)
				.build();
		when(ind.getConfiguration()).thenReturn(cfg);
		when(ind.dependencies()).thenReturn(List.of());

		assertThatThrownBy(() -> registry.registerIndicator(ind))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("指标回溯长度必须大于0");
	}

	@Test
	void testOnBarWithMismatchedIntervalIgnored() {
		Indicator ind = mockIndicator("ind", true);
		registry.registerIndicator(ind);

		Bar bar = Bar.builder()
				.contract(contract)
				.interval(Interval.M5)
				.actionTimestamp(1000L)
				.build();
		registry.onBar(bar);

		verify(ind, never()).update(any(Num.class));
		verify(strategy, never()).onMergedBar(any(Bar.class));
	}

	@Test
	void testDuplicateBarFiltered() {
		Indicator ind = mockIndicator("ind", true);
		registry.registerIndicator(ind);

		registry.onBar(barOf(2000L, false));
		registry.onBar(barOf(1000L, false));	// 时间戳更早的重复数据应被过滤

		verify(ind, times(1)).update(any(Num.class));
		verify(strategy, times(1)).onMergedBar(any(Bar.class));
	}

	@Test
	void testUnstableBarUpdatesIndicatorButNotDispatched() {
		Indicator ind = mockIndicator("ind", true);
		registry.registerIndicator(ind);

		registry.onBar(barOf(1000L, true));

		verify(ind, times(1)).update(any(Num.class));				// 未确定的BAR仍更新指标
		verify(strategy, never()).onMergedBar(any(Bar.class));		// 但不分发给策略
	}

	@Test
	void testVisulizeIndicatorVOCarriesGroupAndPlacement() {
		Indicator ind = mock(Indicator.class);
		Configuration cfg = Configuration.builder()
				.indicatorName("MACD_diff")
				.contract(contract)
				.interval(Interval.M1)
				.valueType(ValueType.CLOSE)
				.cacheLength(5)
				.visible(true)
				.group("MACD")
				.placement(IndicatorPlacement.SUB)
				.build();
		when(ind.getConfiguration()).thenReturn(cfg);
		when(ind.dependencies()).thenReturn(List.of());
		when(strategy.customIndicators()).thenReturn(List.of());
		registry.registerIndicator(ind);

		List<ContractChartDescription> charts = registry.visulize();

		assertThat(charts).hasSize(1);
		assertThat(charts.get(0).getIndicators()).hasSize(1);
		IndicatorVO vo = charts.get(0).getIndicators().get(0);
		assertThat(vo.getName()).isEqualTo("MACD_diff");
		assertThat(vo.getGroup()).isEqualTo("MACD");
		assertThat(vo.getPlacement()).isEqualTo(IndicatorPlacement.SUB);
	}
}
