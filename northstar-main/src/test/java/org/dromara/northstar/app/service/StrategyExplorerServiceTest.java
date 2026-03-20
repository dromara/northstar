package org.dromara.northstar.app.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import org.dromara.northstar.common.model.BacktestModuleTradeDetailsVO;
import org.dromara.northstar.common.model.BacktestModuleVO;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.ExplorerTaskDescription;
import org.dromara.northstar.common.model.ExplorerTaskVO;
import org.dromara.northstar.common.model.StrategyMetaInfo;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.TimeSlot;
import org.dromara.northstar.common.model.core.TradeTimeDefinition;
import org.dromara.northstar.data.jdbc.ExplorerTaskRepository;
import org.dromara.northstar.data.jdbc.ModuleDealRecordRepository;
import org.dromara.northstar.data.jdbc.entity.ExplorerTaskDO;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.dromara.northstar.gateway.mktdata.NorthstarDataSource;
import org.dromara.northstar.module.BacktestModuleFactory;
import org.dromara.northstar.module.BacktestRunner;
import org.dromara.northstar.strategy.example.DualAccountExampleStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StrategyExplorerServiceTest {

    @Mock
    private ExplorerTaskRepository repo;
    
    @Mock
    private ModuleDealRecordRepository dealRepo;
    
    @Mock
    private NorthstarDataSource ds;

    private BacktestModuleFactory factory;
    
    @Mock
    private MarketCenter mktCenter;

    private StrategyExplorerService service = new StrategyExplorerService();
    
    Contract contract = Contract.builder()
    		.unifiedSymbol("ao0000@SHFE@FUTURES")
    		.name("ao0000")
    		.symbol("ao0000@SHFE@FUTURES")
    		.gatewayId("CTP")
    		.lastTradeDate(LocalDate.of(2026, 1, 1))
    		.contractDefinition(ContractDefinition.builder()
    				.tradeTimeDef(TradeTimeDefinition.builder()
    						.timeSlots(List.of(TimeSlot.builder()
    								.start(LocalTime.of(9, 0))
    								.end(LocalTime.of(15, 0))
    								.build()))
    						.build())
    				.build())
    		.build();
    
    IContract ic = mock(IContract.class);

    private ExplorerTaskDescription taskDescription = ExplorerTaskDescription.builder()
    		.taskName("testTask")
    		.preloadFrom("2025-01-01")
    		.startDate("2025-04-01")
    		.endDate("2025-06-30")
    		.contracts(List.of(ContractSimpleInfo.builder().id("ao0000@SHFE@FUTURES@PLAYBACK").unifiedSymbol("ao0000@SHFE@FUTURES").name("ao0000").build()))
    		.periods(List.of(10))
    		.strategySetting(StrategyMetaInfo.builder()
    				.className(DualAccountExampleStrategy.class.getName())
    				.settings(Collections.emptyList())
    				.build())
    		.build();
    
    @Mock
    private BacktestRunner runner;

    @BeforeEach
    void setUp() {
    	factory = new BacktestModuleFactory();
    	factory.setDataSource(ds);
    	factory.setMktCenter(mktCenter);
    	factory.setDealRecordRepo(dealRepo);
    	service.factory = factory;
    	service.repo = repo;
    	when(ic.contract()).thenReturn(contract);
    	when(mktCenter.getContract(any())).thenReturn(ic);
        when(ds.getDailyData(any(), any(), any())).thenReturn(List.of(Bar.builder().closePrice(1000D).build(), Bar.builder().closePrice(1000D).build()));
//        when(dealRepo.findByModuleName(any())).thenReturn(Collections.emptyList());
    }

    @Test
    void testCreateTask() {
        List<ExplorerTaskVO> result = service.createTask(taskDescription);
        verify(repo).save(any(ExplorerTaskDO.class));
        assertNotNull(result);
    }

    @Test
    void testRemoveTask() {
        service.createTask(taskDescription);
        List<ExplorerTaskVO> result = service.removeTask("testTask");
        verify(repo).deleteById("testTask");
        assertNotNull(result);
    }

    @Test
    void testGetTasks() {
        service.createTask(taskDescription);
        List<ExplorerTaskVO> result = service.getTasks();
        assertEquals(1, result.size());
    }

    @Test
    void testRunTask() {
    	assertDoesNotThrow(() -> {
    		service.createTask(taskDescription);
    		service.runTask("testTask");
    	});
    }

    @Test
    void testGetTaskDetails() {
        service.createTask(taskDescription);
        List<BacktestModuleVO> result = service.getTaskDetails("testTask");
        assertEquals(1, result.size());
    }

    @Test
    void testGetModuleTradeDetails() {
        service.createTask(taskDescription);
        BacktestModuleTradeDetailsVO result = service.getModuleTradeDetails("Test_testTask_ao0000_10m");
        assertNotNull(result);
    }


}
