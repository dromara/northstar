package org.dromara.northstar.app.restful;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.dromara.northstar.NorthstarApplication;
import org.dromara.northstar.app.rl.RLTrainingService;
import org.dromara.northstar.common.model.rl.RLModelVO;
import org.dromara.northstar.common.model.rl.RLRoundMetrics;
import org.dromara.northstar.common.model.rl.RLStrategyVO;
import org.dromara.northstar.common.model.rl.RLTrainingStatus;
import org.dromara.northstar.common.model.rl.RLTrainingTaskDTO;
import org.dromara.northstar.common.model.rl.RLTrainingTaskDetailVO;
import org.dromara.northstar.common.model.rl.RLTrainingTaskVO;
import org.dromara.northstar.rl.strategy.RlTrendStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.alibaba.fastjson2.JSON;

@SpringBootTest(classes = NorthstarApplication.class, value = "spring.profiles.active=unittest")
@AutoConfigureMockMvc
class RLControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private RLTrainingService service;

	private final RLTrainingTaskVO taskVO = RLTrainingTaskVO.builder()
			.taskId("task-1")
			.strategyClass(RlTrendStrategy.class.getName())
			.strategyName("RL趋势策略")
			.modelId("rl-trend-dqn")
			.modelName("rl-trend-dqn")
			.algo("DQN")
			.baseVersion(0)
			.targetVersion(1)
			.contracts(List.of("rb0000@SHFE@FUTURES"))
			.warmupStartDate("2022-01-01")
			.trainStartDate("2023-01-01")
			.validationStartDate("2024-07-01")
			.totalRounds(30)
			.episodesPerRound(20)
			.status(RLTrainingStatus.CREATED)
			.currentRound(3)
			.createdAt(1700000000000L)
			.hyperParams(Map.of("lr", 0.0005, "gamma", 0.99))
			.latestAvgReward(0.12)
			.latestLoss(0.034)
			.build();

	@Test
	void listRLStrategies() throws Exception {
		when(service.rlStrategies()).thenReturn(List.of(RLStrategyVO.builder()
				.name("RL趋势策略").className(RlTrendStrategy.class.getName()).build()));
		mockMvc.perform(get("/northstar/rl/strategies"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("RL趋势策略"))
				.andExpect(jsonPath("$[0].className").value(RlTrendStrategy.class.getName()));
	}

	@Test
	void listModels() throws Exception {
		when(service.models()).thenReturn(List.of(RLModelVO.builder()
				.modelId("rl-trend-dqn").name("rl-trend-dqn").algo("DQN")
				.strategyClass(RlTrendStrategy.class.getName())
				.network(RLModelVO.NetworkVO.builder().hidden(List.of(128, 128)).activation("relu").build())
				.hyperParamFields(List.of())
				.versionDetails(List.of(RLModelVO.VersionMetaVO.builder().version(1).hyperParams(Map.of("lr", 0.0005)).build()))
				.latestVersion(1)
				.build()));
		mockMvc.perform(get("/northstar/rl/models"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].modelId").value("rl-trend-dqn"))
				.andExpect(jsonPath("$[0].network.hidden[0]").value(128))
				.andExpect(jsonPath("$[0].versionDetails[0].version").value(1))
				.andExpect(jsonPath("$[0].latestVersion").value(1));
	}

	@Test
	void createTask() throws Exception {
		when(service.createTask(any(RLTrainingTaskDTO.class))).thenReturn(taskVO);
		RLTrainingTaskDTO dto = RLTrainingTaskDTO.builder()
				.strategyClass(RlTrendStrategy.class.getName())
				.modelId("rl-trend-dqn")
				.baseVersion(0)
				.contracts(List.of("rb0000@SHFE@FUTURES"))
				.warmupStartDate("2022-01-01")
				.trainStartDate("2023-01-01")
				.validationStartDate("2024-07-01")
				.totalRounds(30)
				.episodesPerRound(20)
				.initBalance(100000d)
				.maxHoldingBars(240)
				.ruinThreshold(0.5)
				.maxEpisodeBars(2000)
				.hyperParams(Map.of("lr", 0.0005))
				.build();
		mockMvc.perform(post("/northstar/rl/training")
						.contentType(MediaType.APPLICATION_JSON)
						.content(JSON.toJSONString(dto)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.taskId").value("task-1"))
				.andExpect(jsonPath("$.status").value("CREATED"))
				.andExpect(jsonPath("$.targetVersion").value(1))
				.andExpect(jsonPath("$.latestAvgReward").value(0.12));
	}

	@Test
	void listTasks() throws Exception {
		when(service.listTasks()).thenReturn(List.of(taskVO));
		mockMvc.perform(get("/northstar/rl/training"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].taskId").value("task-1"))
				.andExpect(jsonPath("$[0].currentRound").value(3));
	}

	@Test
	void taskDetail() throws Exception {
		when(service.detail("task-1")).thenReturn(RLTrainingTaskDetailVO.builder()
				.task(taskVO)
				.metrics(List.of(RLRoundMetrics.builder()
						.round(1).avgReward(0.1).cumulativeReward(2.5).loss(0.05)
						.epsilon(0.9).lr(0.0005).avgDecisionInterval(12.5)
						.build()))
				.build());
		mockMvc.perform(get("/northstar/rl/training/task-1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.task.taskId").value("task-1"))
				.andExpect(jsonPath("$.metrics[0].round").value(1))
				.andExpect(jsonPath("$.metrics[0].avgDecisionInterval").value(12.5));
	}

	@Test
	void updateParams() throws Exception {
		when(service.updateParams(eq("task-1"), anyMap())).thenReturn(taskVO);
		mockMvc.perform(put("/northstar/rl/training/task-1/params")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"lr\":0.001}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.taskId").value("task-1"));
		verify(service).updateParams(eq("task-1"), anyMap());
	}

	@Test
	void loadDataAndStart() throws Exception {
		when(service.loadData("task-1")).thenReturn(taskVO);
		when(service.start("task-1")).thenReturn(taskVO);
		mockMvc.perform(get("/northstar/rl/training/task-1/load-data")).andExpect(status().isOk());
		mockMvc.perform(get("/northstar/rl/training/task-1/start")).andExpect(status().isOk());
		verify(service).loadData("task-1");
		verify(service).start("task-1");
	}

	@Test
	void deleteTask() throws Exception {
		doNothing().when(service).deleteTask("task-1");
		mockMvc.perform(delete("/northstar/rl/training/task-1")).andExpect(status().isOk());
		verify(service).deleteTask("task-1");
	}

	@Test
	void pauseResumeStop() throws Exception {
		when(service.pause("task-1")).thenReturn(taskVO);
		when(service.resume("task-1")).thenReturn(taskVO);
		when(service.stop("task-1")).thenReturn(taskVO);
		mockMvc.perform(get("/northstar/rl/training/task-1/pause")).andExpect(status().isOk());
		mockMvc.perform(get("/northstar/rl/training/task-1/resume")).andExpect(status().isOk());
		mockMvc.perform(get("/northstar/rl/training/task-1/stop")).andExpect(status().isOk());
		verify(service).pause("task-1");
		verify(service).resume("task-1");
		verify(service).stop("task-1");
	}
}
