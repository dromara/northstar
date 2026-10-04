package org.dromara.northstar.app.rl;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.TradeTimeDefinition;
import org.dromara.northstar.common.model.core.TimeSlot;
import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.rl.agent.RLAgent;
import org.dromara.northstar.rl.experience.ExperienceStore;
import org.dromara.northstar.rl.strategy.RlTrendStrategy;
import org.dromara.northstar.strategy.rl.TradeAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;

/**
 * TradingEnvironment 单测：合成行情 + 真实沙箱 + 随机策略（无模型文件冷启动），
 * 验证事件驱动决策主循环产出的 transition 契约。
 */
class TradingEnvironmentTest {

	@TempDir
	Path modelDir;

	private Contract contract;
	private IContract gatewayContract;
	private List<Bar> bars;

	@BeforeEach
	void setUp() throws Exception {
		Files.createDirectories(modelDir.resolve("versions/v1/rounds"));
		Files.writeString(modelDir.resolve("metadata.json"), """
				{"name":"rl-trend-dqn","strategy_class":"org.dromara.northstar.rl.strategy.RlTrendStrategy",
				 "algo":"DQN","state_dim":6,"action_dim":3,"actions":["HOLD","BUY","SELL"],
				 "network":{"hidden":[128,128]},"preprocessing":{"type":"identity"},
				 "spec_hash":"x","latest_version":1}
				""");
		Files.writeString(modelDir.resolve("versions/v1/metadata.json"), """
				{"version":1,"parent_version":0,"weight_inherited":false,"spec_hash":"x",
				 "preprocessing":{"type":"identity"},"training":{"rounds":2,"hyper_params":{},"seed":1}}
				""");

		contract = Contract.builder()
				.contractId("rb0000@SHFE@FUTURES@PLAYBACK")
				.unifiedSymbol("rb0000@SHFE@FUTURES")
				.symbol("rb0000")
				.name("螺纹钢指数")
				.exchange(ExchangeEnum.SHFE)
				.productClass(ProductClassEnum.FUTURES)
				.multiplier(10)
				.priceTick(1.0)
				.channelType(ChannelType.PLAYBACK)
				.contractDefinition(ContractDefinition.builder()
						.marginRatio(0.08)
						.commissionFee(5.0)
						.tradeTimeDef(TradeTimeDefinition.builder()
								.timeSlots(List.of(TimeSlot.builder()
										.start(LocalTime.of(9, 0))
										.end(LocalTime.of(15, 0))
										.build()))
								.build())
						.build())
				.build();

		gatewayContract = Mockito.mock(IContract.class);
		Mockito.when(gatewayContract.contract()).thenReturn(contract);
		Mockito.when(gatewayContract.dataSource()).thenReturn(Mockito.mock(IDataSource.class));

		// 3 天 × 每天 120 根 M1（10:01 起逐分钟），价格 = 趋势 + 正弦波动
		bars = new ArrayList<>();
		for (int d = 0; d < 3; d++) {
			LocalDate day = LocalDate.of(2024, 1, 2).plusDays(d);
			for (int i = 0; i < 120; i++) {
				LocalTime time = LocalTime.of(10, 1).plusMinutes(i);
				long ts = day.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
				double price = 3000 + d * 100 + i * 0.5 + 30 * Math.sin((d * 120 + i) / 8.0);
				bars.add(Bar.builder()
						.contract(contract)
						.actionDay(day)
						.tradingDay(day)
						.actionTime(time)
						.actionTimestamp(ts)
						.openPrice(price - 2).highPrice(price + 5)
						.lowPrice(price - 5).closePrice(price)
						.volume(1000).volumeDelta(10)
						.openInterest(5000).openInterestDelta(1)
						.turnover(1e6).turnoverDelta(1e4)
						.interval(Interval.M1)
						.channelType(ChannelType.PLAYBACK)
						.build());
			}
		}
	}

	private TradingEnvironment newEnv(RLAgent agent) {
		// maxHoldingBars 取 10：初始决策点改为"指标就绪或强触发上限"后才观测（预热第四层收口），
		// 上限须小于单局 bar 数上限(120)，否则低波动合成行情下整局无决策点、零采样
		return new TradingEnvironment(gatewayContract, bars, 120, 300, new RlTrendStrategy(), agent, 0,
				"rl-test", new TradingEnvironment.RLEnvConfig(100_000, 60, 10, 0.5, 120));
	}

	@Test
	void sampleProducesTransitionsWithDtAndReward() throws Exception {
		try (ExperienceStore store = new ExperienceStore(modelDir.resolve("versions/v1/rounds"), 6);
			 RLAgent agent = new RLAgent(modelDir, 1)) {
			TradingEnvironment env = newEnv(agent);
			TradingEnvironment.SampleStats stats = env.sample(new Random(42), 2, 1.0, 0, store);
			store.flush();

			assertTrue(stats.transitions() > 0, "应产生 transition");
			assertTrue(stats.totalDt() >= stats.transitions(), "dt 至少为 1");
			Path shard = modelDir.resolve("versions/v1/rounds/replay-r0-c0.csv");
			assertTrue(Files.exists(shard));
			List<String> lines = Files.readAllLines(shard);
			assertEquals(ExperienceStore.header(6), lines.get(0));
			assertEquals(stats.transitions(), lines.size() - 1, "每决策点一条样本");
			for (String line : lines.subList(1, lines.size())) {
				String[] cols = line.split(",");
				assertEquals(16, cols.length, "2*stateDim+4 列契约");
				assertTrue(Integer.parseInt(cols[14]) >= 1, "dt 为正");
				assertTrue("0".equals(cols[15]) || "1".equals(cols[15]));
			}
		}
	}

	@Test
	void sampleIsReproducibleWithSameSeed() throws Exception {
		try (RLAgent agent = new RLAgent(modelDir, 1)) {
			TradingEnvironment env = newEnv(agent);
			try (ExperienceStore store1 = new ExperienceStore(modelDir.resolve("versions/v1/rounds"), 6)) {
				env.sample(new Random(7), 2, 1.0, 0, store1);
			}
			List<String> first = Files.readAllLines(modelDir.resolve("versions/v1/rounds/replay-r0-c0.csv"));

			Files.delete(modelDir.resolve("versions/v1/rounds/replay-r0-c0.csv"));
			try (ExperienceStore store2 = new ExperienceStore(modelDir.resolve("versions/v1/rounds"), 6)) {
				env.sample(new Random(7), 2, 1.0, 0, store2);
			}
			List<String> second = Files.readAllLines(modelDir.resolve("versions/v1/rounds/replay-r0-c0.csv"));
			assertEquals(first, second, "同一 seed 应完全复现采样序列");
		}
	}

	@Test
	void evaluateReturnsNullWhenModelNotReady() throws Exception {
		try (RLAgent agent = new RLAgent(modelDir, 1)) {
			assertNull(newEnv(agent).evaluate(), "无模型文件时不做评估");
		}
	}

	/** 以固定动作返回的 mock Agent（模型就绪，贪心/利用路径直接命中） */
	private RLAgent fixedActionAgent(TradeAction action) {
		RLAgent agent = Mockito.mock(RLAgent.class);
		Mockito.when(agent.isReady()).thenReturn(true);
		Mockito.when(agent.act(Mockito.any())).thenReturn(action);
		return agent;
	}

	/** 合成行情：每个交易日 barsPerDay 根 M1，价格由 priceFn(全局序号) 决定 */
	private List<Bar> synthBars(int days, int barsPerDay, java.util.function.IntUnaryOperator priceFn) {
		List<Bar> result = new ArrayList<>();
		for (int d = 0; d < days; d++) {
			LocalDate day = LocalDate.of(2024, 1, 2).plusDays(d);
			for (int i = 0; i < barsPerDay; i++) {
				double price = priceFn.applyAsInt(d * barsPerDay + i);
				LocalTime time = LocalTime.of(10, 1).plusMinutes(i);
				long ts = day.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
				result.add(Bar.builder()
						.contract(contract)
						.actionDay(day)
						.tradingDay(day)
						.actionTime(time)
						.actionTimestamp(ts)
						.openPrice(price).highPrice(price + 2)
						.lowPrice(price - 2).closePrice(price)
						.volume(1000).volumeDelta(10)
						.openInterest(5000).openInterestDelta(1)
						.turnover(1e6).turnoverDelta(1e4)
						.interval(Interval.M1)
						.channelType(ChannelType.PLAYBACK)
						.build());
			}
		}
		return result;
	}

	/** 读取指定轮次分片的全部数据行（跳过表头） */
	private List<String[]> shardRows(Path roundsDir, int round) throws Exception {
		Path shard = roundsDir.resolve("replay-r%d-c0.csv".formatted(round));
		assertTrue(Files.exists(shard), "经验分片应存在");
		List<String> lines = Files.readAllLines(shard);
		return lines.subList(1, lines.size()).stream().map(l -> l.split(",")).toList();
	}

	@Test
	void ruinTriggersTerminatedAndEarlyExit() throws Exception {
		// 单边下跌行情 + 永远做多：权益必然击穿阈值，触发 terminated 真失败。
		// 固定随机源使 episode 起点确定（nextInt 恒 0 → 从训练段起点开始）
		Random fixedZero = new Random() {
			@Override
			public int nextInt(int bound) {
				return 0;
			}
		};
		List<Bar> downBars = synthBars(1, 100, i -> 4000 - i * 30);
		TradingEnvironment env = new TradingEnvironment(gatewayContract, downBars, 20, 99,
				new RlTrendStrategy(), fixedActionAgent(TradeAction.BUY), 0, "rl-ruin",
				new TradingEnvironment.RLEnvConfig(100_000, 20, 5, 0.02, 500));

		TradingEnvironment.SampleStats stats;
		List<String[]> rows;
		try (ExperienceStore store = new ExperienceStore(modelDir.resolve("versions/v1/rounds"), 6)) {
			stats = env.sample(fixedZero, 1, 0.0, 9, store);
			store.flush();
			rows = shardRows(modelDir.resolve("versions/v1/rounds"), 9);
		}

		assertTrue(stats.transitions() > 0, "击穿前应产生若干 transition");
		assertEquals(stats.transitions(), rows.size(), "每决策点一条样本");
		assertEquals("1", rows.getLast()[15], "权益击穿：最后一条 transition 必须为 terminated=1");
		for (int i = 0; i < rows.size() - 1; i++) {
			assertEquals("0", rows.get(i)[15], "击穿前的 transition 均为普通决策点");
		}
		// 击穿提前结束：消耗的 bar 数（dt 合计）远小于训练段长度（79 根）
		assertTrue(stats.totalDt() < 30, "权益击穿后回合应提前结束，实际 dt 合计: " + stats.totalDt());
	}

	@Test
	void maxHoldingBarsCapsDecisionInterval() throws Exception {
		// 无波动行情（shouldObserve 恒 false）：所有决策点只能由超时强触发产生，dt 被封顶
		List<Bar> flatBars = synthBars(3, 100, _ -> 3000);
		TradingEnvironment env = new TradingEnvironment(gatewayContract, flatBars, 20, 299,
				new RlTrendStrategy(), fixedActionAgent(TradeAction.HOLD), 0, "rl-flat",
				new TradingEnvironment.RLEnvConfig(100_000, 20, 10, 0.9, 500));

		TradingEnvironment.SampleStats stats;
		List<String[]> rows;
		try (ExperienceStore store = new ExperienceStore(modelDir.resolve("versions/v1/rounds"), 6)) {
			stats = env.sample(new Random(2), 1, 0.0, 8, store);
			store.flush();
			rows = shardRows(modelDir.resolve("versions/v1/rounds"), 8);
		}

		assertTrue(stats.transitions() > 3, "死行情下仍应靠超时强触发采样");
		for (String[] cols : rows) {
			int dt = Integer.parseInt(cols[14]);
			assertTrue(dt >= 1 && dt <= 10, "dt 应被 maxHoldingBars 封顶，实际: " + dt);
			assertEquals("0", cols[15], "无击穿，全部 transition 均为非 terminated");
		}
	}

	@Test
	void exhaustionRecordsTruncatedTransition() throws Exception {
		// 温和波动行情 + 做多持有 + 高击穿阈值：回合必然走到数据耗尽（truncated 而非 terminated）
		TradingEnvironment env = newEnv(fixedActionAgent(TradeAction.BUY));

		TradingEnvironment.SampleStats stats;
		List<String[]> rows;
		try (ExperienceStore store = new ExperienceStore(modelDir.resolve("versions/v1/rounds"), 6)) {
			stats = env.sample(new Random(3), 1, 0.0, 7, store);
			store.flush();
			rows = shardRows(modelDir.resolve("versions/v1/rounds"), 7);
		}

		assertTrue(stats.transitions() > 0);
		assertEquals(stats.transitions(), rows.size());
		for (String[] cols : rows) {
			assertEquals("0", cols[15], "数据耗尽为 truncated 语义：terminated 恒为 0");
		}
		// 耗尽时强制平仓收尾：最后一条 transition 记录的是耗尽边界的结算
		assertTrue(Integer.parseInt(rows.getLast()[14]) >= 1, "末尾强平 transition 的 dt 为正");
	}

	@Test
	void evaluateProducesDealsAndMetricsWithReadyModel() {
		TradingEnvironment env = newEnv(fixedActionAgent(TradeAction.BUY));

		TradingEnvironment.EvalOutcome outcome = env.evaluate();

		assertNotNull(outcome, "模型就绪时应产出评估结果");
		assertEquals(100_000, outcome.initBalance());
		assertFalse(outcome.deals().isEmpty(), "验证时段末尾强平应产出成交记录");
		assertTrue(outcome.finalEquity() > 0, "评估应给出期末权益");
	}
}
