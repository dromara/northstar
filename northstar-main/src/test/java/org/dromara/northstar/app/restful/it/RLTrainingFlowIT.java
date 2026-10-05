package org.dromara.northstar.app.restful.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.TimeSlot;
import org.dromara.northstar.common.model.core.TradeTimeDefinition;
import org.dromara.northstar.common.model.rl.RLTrainingTaskDTO;
import org.dromara.northstar.gateway.mktdata.NorthstarDataSource;
import org.dromara.northstar.gateway.playback.PlaybackContract;
import org.dromara.northstar.rl.model.RLModelStore;
import org.dromara.northstar.rl.training.RLTrainExecutor;
import org.dromara.northstar.strategy.example.RlTestStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;

/**
 * RL 训练链路端到端测试（Controller -> Service -> Runner -> 环境 -> 执行器）。
 *
 * 外部依赖替身：NorthstarDataSource 提供合成行情（确定性正弦波），
 * RLTrainExecutor 桩掉 Python 子进程（不加载 PyTorch）。
 * 模型根目录指向测试临时目录（任务创建的版本目录只增不改，避免污染仓库种子模型）。
 */
class RLTrainingFlowIT extends RestE2ESupport {

	private static final String SYMBOL = "rb0000@SHFE@FUTURES";

	@MockitoBean
	private NorthstarDataSource dataSource;

	@MockitoBean
	private RLTrainExecutor trainExecutor;

	private static Path modelsRoot;
	private static Path dataRoot;

	@org.springframework.test.context.DynamicPropertySource
	static void rlProps(org.springframework.test.context.DynamicPropertyRegistry registry) throws Exception {
		modelsRoot = Files.createTempDirectory("rl-it-models");
		dataRoot = Files.createTempDirectory("rl-it-data");
		// 种子模型：metadata 即传输载荷，由策略现场 spec 引导生成（specHash 与策略实时计算值一致）
		RLModelStore store = new RLModelStore(modelsRoot);
		store.syncModel(new RlTestStrategy());
		registry.add("northstar.rl.models-root", () -> modelsRoot.toString());
		registry.add("northstar.rl.data-root", () -> dataRoot.toString());
	}

	private final Contract contract = Contract.builder()
			.contractId(SYMBOL + "@INDEX")
			.unifiedSymbol(SYMBOL)
			.symbol("rb0000")
			.name("螺纹钢0000")
			.fullName("螺纹钢0000")
			.thirdPartyId("rb0000")
			.lastTradeDate(LocalDate.of(2099, 1, 1))
			.exchange(xyz.redtorch.pb.CoreEnum.ExchangeEnum.SHFE)
			.productClass(xyz.redtorch.pb.CoreEnum.ProductClassEnum.FUTURES)
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

	@BeforeEach
	void setUp() throws Exception {
		when(dataSource.getAllContracts()).thenReturn(List.of(contract));
		when(dataSource.getData(any(Contract.class), any(Interval.class), any(LocalDate.class), any(LocalDate.class)))
				.thenAnswer(inv -> {
					LocalDate from = inv.getArgument(2);
					LocalDate to = inv.getArgument(3);
					return syntheticBars(from, to);	// 数据源返回时间降序
				});
		when(trainExecutor.trainRound(any(Path.class), anyInt(), anyInt(), any(), anyLong()))
				.thenReturn(new RLTrainExecutor.RLTrainResult(0.123, 300, 100));
		// 显式注册回放合约（应用启动时数据源替身尚未打桩，PlaybackGatewayFactory 不会注册任何合约）
		mktCenter.addInstrument(new PlaybackContract(contract, dataSource));
		mktCenter.loadContractGroup(ChannelType.PLAYBACK);
	}

	@AfterEach
	void cleanUp() throws Exception {
		reset();
	}

	/** 确定性合成行情：2024-01-01 ~ 2024-03-31，每日 100 根 M1（10:01 起），趋势+正弦波动 */
	private List<Bar> syntheticBars(LocalDate from, LocalDate to) {
		List<Bar> result = new ArrayList<>();
		LocalDate dataEnd = LocalDate.of(2024, 3, 31);
		for (LocalDate day = from; !day.isAfter(to) && !day.isAfter(dataEnd); day = day.plusDays(1)) {
			if (day.isBefore(LocalDate.of(2024, 1, 1))) {
				continue;
			}
			for (int i = 0; i < 100; i++) {
				LocalTime time = LocalTime.of(10, 1).plusMinutes(i);
				long ts = day.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
				int dayIdx = (int) (day.toEpochDay() - LocalDate.of(2024, 1, 1).toEpochDay());
				double price = 3000 + dayIdx * 2 + i * 0.05 + 20 * Math.sin((dayIdx * 100 + i) / 10.0);
				result.add(Bar.builder()
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
		result.sort(Comparator.comparingLong(Bar::actionTimestamp).reversed());
		return result;
	}

	private RLTrainingTaskDTO taskDTO(int rounds) {
		return RLTrainingTaskDTO.builder()
				.strategyClass("org.dromara.northstar.strategy.example.RlTestStrategy")
				.modelId("dqn-demo")
				.baseVersion(0)
				.contracts(List.of(SYMBOL))
				.warmupStartDate("2024-01-01")
				.trainStartDate("2024-02-01")
				.validationStartDate("2024-03-01")
				.totalRounds(rounds)
				.episodesPerRound(2)
				.initBalance(100000d)
				.maxHoldingBars(240)
				.ruinThreshold(0.5)
				.maxEpisodeBars(2000)
				.hyperParams(Map.of("lr", 0.0005))
				.build();
	}

	private String createTask(Auth auth, RLTrainingTaskDTO dto) throws Exception {
		MvcResult result = mvc.perform(authed(auth, post("/northstar/rl/training")
						.contentType(org.springframework.http.MediaType.APPLICATION_JSON)
						.content(JSON.toJSONString(dto))))
				.andExpect(status().isOk())
				.andReturn();
		JSONObject body = JSON.parseObject(result.getResponse().getContentAsString());
		assertThat(body.getString("status")).isEqualTo("CREATED");
		return body.getString("taskId");
	}

	/** 加载数据并等待就绪（READY）；加载失败（回到 CREATED 且带 failReason）时提前暴露 */
	private void loadDataUntilReady(Auth auth, String taskId) throws Exception {
		mvc.perform(authed(auth, get("/northstar/rl/training/" + taskId + "/load-data")))
				.andExpect(status().isOk());
		awaitCondition(() -> {
			try {
				JSONObject task = detail(auth, taskId).getJSONObject("task");
				return "READY".equals(task.getString("status"))
						|| "CREATED".equals(task.getString("status")) && task.getString("failReason") != null;
			} catch (Exception e) {
				return false;
			}
		}, "RL 数据加载应进入 READY");
		assertThat(detail(auth, taskId).getJSONObject("task").getString("status")).isEqualTo("READY");
	}

	private void startTask(Auth auth, String taskId) throws Exception {
		mvc.perform(authed(auth, get("/northstar/rl/training/" + taskId + "/start")))
				.andExpect(status().isOk());
	}

	private JSONObject detail(Auth auth, String taskId) throws Exception {
		MvcResult result = mvc.perform(authed(auth, get("/northstar/rl/training/" + taskId)))
				.andExpect(status().isOk())
				.andReturn();
		return JSON.parseObject(result.getResponse().getContentAsString());
	}

	@Test
	void catalogEndpoints() throws Exception {
		Auth auth = login();

		MvcResult strategies = mvc.perform(authed(auth, get("/northstar/rl/strategies")))
				.andExpect(status().isOk()).andReturn();
		assertThat(strategies.getResponse().getContentAsString()).contains("RlTestStrategy");

		MvcResult models = mvc.perform(authed(auth, get("/northstar/rl/models")))
				.andExpect(status().isOk()).andReturn();
		String body = models.getResponse().getContentAsString();
		assertThat(body).contains("dqn-demo").contains("hyperParamFields").contains("lr");
	}

	@Test
	void fullTrainingLifecycle() throws Exception {
		Auth auth = login();
		String taskId = createTask(auth, taskDTO(2));
		assertThat(taskId).isNotBlank();

		// 数据加载：从数据源拉取并落地 CSV
		loadDataUntilReady(auth, taskId);
		assertThat(dataRoot.resolve(taskId).resolve(SYMBOL + ".csv")).exists();

		// 重复触发加载已被 READY 状态拒绝（数据仅加载一次）
		MvcResult reload = mvc.perform(authed(auth, get("/northstar/rl/training/" + taskId + "/load-data")))
				.andReturn();
		assertThat(reload.getResponse().getStatus()).isNotEqualTo(200);

		// 动态调参（READY 状态，启动训练后生效）
		mvc.perform(authed(auth, put("/northstar/rl/training/" + taskId + "/params")
						.contentType(org.springframework.http.MediaType.APPLICATION_JSON)
						.content("{\"lr\":0.002}")))
				.andExpect(status().isOk());
		assertThat(detail(auth, taskId).getJSONObject("task").getJSONObject("hyperParams")
				.getDoubleValue("lr")).isEqualTo(0.002);

		// 启动训练
		startTask(auth, taskId);

		// 等待任务完成（2 轮采样 + 桩训练，合成数据量小，应快速结束）
		awaitCondition(() -> {
			try {
				return "FINISHED".equals(detail(auth, taskId).getJSONObject("task").getString("status"));
			} catch (Exception e) {
				return false;
			}
		}, "RL 训练任务应运行至 FINISHED");

		JSONObject detail = detail(auth, taskId);
		JSONObject task = detail.getJSONObject("task");
		assertThat(task.getIntValue("currentRound")).isEqualTo(2);
		int targetVersion = task.getIntValue("targetVersion");
		assertThat(targetVersion).isGreaterThanOrEqualTo(1);
		assertThat(task.getJSONArray("contracts")).containsExactly(SYMBOL);

		var metrics = detail.getJSONArray("metrics").toJavaList(JSONObject.class);
		assertThat(metrics).hasSize(2);
		assertThat(metrics.get(0).getDoubleValue("loss")).isEqualTo(0.123);	// 桩执行器的返回值
		assertThat(metrics.get(0).getDoubleValue("epsilon")).isEqualTo(1.0);	// 首轮 epsilon=epsilonStart
		assertThat(metrics.get(0).getDoubleValue("avgDecisionInterval")).isGreaterThan(0);

		// 版本目录契约：metadata + rounds 分片
		Path versionDir = modelsRoot.resolve("dqn-demo/versions/v" + targetVersion);
		assertThat(versionDir.resolve("metadata.json")).exists();
		assertThat(Files.list(versionDir.resolve("rounds")).findAny()).isPresent();

		// 已终结的任务拒绝调参
		MvcResult rejected = mvc.perform(authed(auth, put("/northstar/rl/training/" + taskId + "/params")
						.contentType(org.springframework.http.MediaType.APPLICATION_JSON)
						.content("{\"lr\":0.005}")))
				.andReturn();
		assertThat(rejected.getResponse().getStatus()).isNotEqualTo(200);
		assertThat(detail(auth, taskId).getJSONObject("task").getJSONObject("hyperParams")
				.getDoubleValue("lr")).as("拒绝后参数保持训练前取值").isEqualTo(0.002);

		// 任务列表包含最新指标
		MvcResult list = mvc.perform(authed(auth, get("/northstar/rl/training")))
				.andExpect(status().isOk()).andReturn();
		List<JSONObject> tasks = JSON.parseObject(list.getResponse().getContentAsString(),
				new TypeReference<List<JSONObject>>() { });
		JSONObject found = tasks.stream().filter(t -> taskId.equals(t.getString("taskId"))).findFirst().orElseThrow();
		assertThat(found.getString("status")).isEqualTo("FINISHED");
		assertThat(found.getDouble("latestAvgReward")).isNotNull();
	}

	@Test
	void pauseResumeStopLifecycle() throws Exception {
		Auth auth = login();
		// 慢速桩执行器让任务停留在运行中，便于观测暂停/继续
		when(trainExecutor.trainRound(any(Path.class), anyInt(), anyInt(), any(), anyLong()))
				.thenAnswer(_ -> {
					Thread.sleep(300);
					return new RLTrainExecutor.RLTrainResult(0.1, 300, 100);
				});

		String taskId = createTask(auth, taskDTO(100));
		loadDataUntilReady(auth, taskId);
		startTask(auth, taskId);
		awaitCondition(() -> {
			try {
				return "RUNNING".equals(detail(auth, taskId).getJSONObject("task").getString("status"));
			} catch (Exception e) {
				return false;
			}
		}, "任务应进入 RUNNING");

		mvc.perform(authed(auth, get("/northstar/rl/training/" + taskId + "/pause")))
				.andExpect(status().isOk());
		assertThat(detail(auth, taskId).getJSONObject("task").getString("status")).isEqualTo("PAUSED");
		int roundAtPause = detail(auth, taskId).getJSONObject("task").getIntValue("currentRound");

		mvc.perform(authed(auth, get("/northstar/rl/training/" + taskId + "/resume")))
				.andExpect(status().isOk());
		assertThat(detail(auth, taskId).getJSONObject("task").getString("status")).isEqualTo("RUNNING");

		mvc.perform(authed(auth, get("/northstar/rl/training/" + taskId + "/stop")))
				.andExpect(status().isOk());
		awaitCondition(() -> {
			try {
				JSONObject t = detail(auth, taskId).getJSONObject("task");
				return "FINISHED".equals(t.getString("status"));
			} catch (Exception e) {
				return false;
			}
		}, "停止后任务应完结");
		assertThat(detail(auth, taskId).getJSONObject("task").getIntValue("currentRound"))
				.isLessThan(100)
				.isGreaterThanOrEqualTo(roundAtPause);
	}

	@Test
	void rejectsInvalidCreation() throws Exception {
		Auth auth = login();

		// 非 RL 策略
		RLTrainingTaskDTO badStrategy = taskDTO(2);
		badStrategy.setStrategyClass("org.dromara.northstar.strategy.example.BeginnerExampleStrategy");
		MvcResult r1 = mvc.perform(authed(auth, post("/northstar/rl/training")
						.contentType(org.springframework.http.MediaType.APPLICATION_JSON)
						.content(JSON.toJSONString(badStrategy))))
				.andReturn();
		assertThat(r1.getResponse().getStatus()).isNotEqualTo(200);

		// 日期不递增
		RLTrainingTaskDTO badDates = taskDTO(2);
		badDates.setWarmupStartDate("2024-03-01");
		MvcResult r2 = mvc.perform(authed(auth, post("/northstar/rl/training")
						.contentType(org.springframework.http.MediaType.APPLICATION_JSON)
						.content(JSON.toJSONString(badDates))))
				.andReturn();
		assertThat(r2.getResponse().getStatus()).isNotEqualTo(200);

		// 未认证请求被拦截（e2e profile 开启 CSRF 校验）
		MvcResult r3 = mvc.perform(post("/northstar/rl/training")
						.contentType(org.springframework.http.MediaType.APPLICATION_JSON)
						.content(JSON.toJSONString(taskDTO(2))))
				.andReturn();
		assertThat(r3.getResponse().getStatus()).isEqualTo(403);
	}

	@Test
	void dataLoadFailureReturnsToCreated() throws Exception {
		Auth auth = login();

		// 合约在数据服务中不存在：创建阶段不再触碰数据，应成功创建为 CREATED
		RLTrainingTaskDTO dto = taskDTO(2);
		dto.setContracts(List.of("cu0000@SHFE@FUTURES"));
		String taskId = createTask(auth, dto);

		// 数据加载阶段失败：任务回到 CREATED 并记录原因（可重试）
		mvc.perform(authed(auth, get("/northstar/rl/training/" + taskId + "/load-data")))
				.andExpect(status().isOk());
		awaitCondition(() -> {
			try {
				JSONObject task = detail(auth, taskId).getJSONObject("task");
				return "CREATED".equals(task.getString("status")) && task.getString("failReason") != null;
			} catch (Exception e) {
				return false;
			}
		}, "数据加载失败应回到 CREATED 并记录原因");

		JSONObject task = detail(auth, taskId).getJSONObject("task");
		assertThat(task.getString("failReason")).contains("数据加载失败");

		// 未就绪任务不能启动训练
		MvcResult start = mvc.perform(authed(auth, get("/northstar/rl/training/" + taskId + "/start")))
				.andReturn();
		assertThat(start.getResponse().getStatus()).isNotEqualTo(200);
	}

	@Test
	void deleteTaskLifecycle() throws Exception {
		Auth auth = login();

		// CREATED：删除后任务记录与 fork 的版本一并回收（版本目录只增不改，断言相对增量）
		int latestBefore = modelStoreLatestVersion();
		String taskId = createTask(auth, taskDTO(2));
		mvc.perform(authed(auth, delete("/northstar/rl/training/" + taskId)))
				.andExpect(status().isOk());
		assertThat(taskCount(auth)).isZero();
		assertThat(modelStoreLatestVersion()).isEqualTo(latestBefore);

		// READY：数据文件随任务删除，版本同样回收
		String taskId2 = createTask(auth, taskDTO(2));
		loadDataUntilReady(auth, taskId2);
		assertThat(dataRoot.resolve(taskId2).resolve(SYMBOL + ".csv")).exists();
		mvc.perform(authed(auth, delete("/northstar/rl/training/" + taskId2)))
				.andExpect(status().isOk());
		assertThat(taskCount(auth)).isZero();
		assertThat(dataRoot.resolve(taskId2)).doesNotExist();
		assertThat(modelStoreLatestVersion()).isEqualTo(latestBefore);
	}

	private int taskCount(Auth auth) throws Exception {
		MvcResult list = mvc.perform(authed(auth, get("/northstar/rl/training")))
				.andExpect(status().isOk()).andReturn();
		return JSON.parseArray(list.getResponse().getContentAsString()).size();
	}

	private int modelStoreLatestVersion() throws Exception {
		Path metaFile = modelsRoot.resolve("dqn-demo/metadata.json");
		return JSON.parseObject(Files.readString(metaFile)).getIntValue("latest_version");
	}
}
