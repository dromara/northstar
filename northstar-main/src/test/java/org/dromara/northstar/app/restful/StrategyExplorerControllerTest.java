package org.dromara.northstar.app.restful;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.dromara.northstar.NorthstarApplication;
import org.dromara.northstar.app.service.StrategyExplorerService;
import org.dromara.northstar.common.model.BacktestModuleTradeDetailsVO;
import org.dromara.northstar.common.model.BacktestModuleVO;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.ExplorerTaskDescription;
import org.dromara.northstar.common.model.ExplorerTaskVO;
import org.dromara.northstar.common.model.StrategyMetaInfo;
import org.dromara.northstar.strategy.example.DualAccountExampleStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.alibaba.fastjson2.JSON;

@SpringBootTest(classes = NorthstarApplication.class, value = "spring.profiles.active=unittest")
@AutoConfigureMockMvc
public class StrategyExplorerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StrategyExplorerService service;

    private ExplorerTaskVO taskVO;
    private BacktestModuleVO moduleVO;
    private BacktestModuleTradeDetailsVO tradeDetailsVO;

    private ExplorerTaskDescription taskDescription = ExplorerTaskDescription.builder()
    		.taskName("testTask")
    		.preloadFrom("2025-01-01")
    		.startDate("2025-04-01")
    		.endDate("2025-06-30")
    		.contracts(List.of(ContractSimpleInfo.builder().id("ao0000@SHFE@FUTURES@PLAYBACK").unifiedSymbol("ao0000@SHFE@FUTURES").name("ao0000").build()))
    		.intervals(List.of("10m"))
    		.strategySetting(StrategyMetaInfo.builder()
    				.className(DualAccountExampleStrategy.class.getName())
    				.settings(Collections.emptyList())
    				.build())
    		.build();
    
    @BeforeEach
    void setUp() {
        taskVO = new ExplorerTaskVO();
        moduleVO = new BacktestModuleVO();
        tradeDetailsVO = new BacktestModuleTradeDetailsVO();
    }

    @Test
    void testCreateTask() throws Exception {
        when(service.createTask(taskDescription)).thenReturn(Arrays.asList(taskVO));
        mockMvc.perform(post("/northstar/explorer/task")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.toJSONString(taskDescription)))
                .andExpect(status().isOk());
    }

    @Test
    void testRemoveTask() throws Exception {
        when(service.removeTask("testTask")).thenReturn(Arrays.asList(taskVO));
        mockMvc.perform(delete("/northstar/explorer/task?taskName=testTask"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetTasks() throws Exception {
        when(service.getTasks()).thenReturn(Arrays.asList(taskVO));
        mockMvc.perform(get("/northstar/explorer/task"))
                .andExpect(status().isOk());
    }

    @Test
    void testRunTask() throws Exception {
        mockMvc.perform(put("/northstar/explorer/task?taskName=testTask"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetTaskDetails() throws Exception {
        when(service.getTaskDetails("testTask")).thenReturn(Arrays.asList(moduleVO));
        mockMvc.perform(get("/northstar/explorer/task/details?taskName=testTask"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetModuleTradeDetails() throws Exception {
        when(service.getModuleTradeDetails("testModule")).thenReturn(tradeDetailsVO);
        mockMvc.perform(get("/northstar/explorer/trades?moduleName=testModule"))
                .andExpect(status().isOk());
    }
}


