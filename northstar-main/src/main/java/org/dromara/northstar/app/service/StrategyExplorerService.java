package org.dromara.northstar.app.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.dromara.northstar.common.exception.OperationException;
import org.dromara.northstar.common.model.BacktestModuleTradeDetailsVO;
import org.dromara.northstar.common.model.BacktestModuleVO;
import org.dromara.northstar.common.model.ExplorerTaskDescription;
import org.dromara.northstar.common.model.ExplorerTaskVO;
import org.dromara.northstar.data.jdbc.ExplorerTaskRepository;
import org.dromara.northstar.data.jdbc.entity.ExplorerTaskDO;
import org.dromara.northstar.module.BacktestModuleFactory;
import org.dromara.northstar.module.BacktestRunner;
import org.dromara.northstar.support.utils.NamingValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

/* StrategyExplorerService主要负责解决持久化与实体管理的问题 */

@Slf4j
@Service
public class StrategyExplorerService implements CommandLineRunner  {
	
	@Autowired
	protected ExplorerTaskRepository repo;
	
	@Autowired
	protected BacktestModuleFactory factory;
	
	private Map<String, BacktestRunner> runnerMap = new HashMap<>();
	
	private Map<String, BacktestRunner> moduleRunnerMap = new HashMap<>();
	
	@Transactional
	public synchronized List<ExplorerTaskVO> createTask(@RequestBody ExplorerTaskDescription task){
		if(!NamingValidator.isValid(task.getTaskName())) {
			throw new OperationException("error.illegalCharater", task.getTaskName());
		}
		log.info("新增策略探索任务：{}", task.getTaskName());
		repo.save(ExplorerTaskDO.convertFrom(task));
		BacktestRunner runner = factory.createTestRunner(task);
		runnerMap.put(task.getTaskName(), runner);
		runner.getModules().forEach(m -> moduleRunnerMap.put(m.getModuleContext().getModuleDescription().getName(), runner));
		return getTasks();
	}
	
	@Transactional
	public synchronized List<ExplorerTaskVO> removeTask(String taskName){
		log.info("移除策略探索任务：{}", taskName);
		repo.deleteById(taskName);
		BacktestRunner runner = runnerMap.remove(taskName);
		runner.getModules().forEach(m -> moduleRunnerMap.remove(m.getModuleContext().getModuleDescription().getName()));
		return getTasks();
	}
	
	public List<ExplorerTaskVO> getTasks(){
		return runnerMap.values().stream().map(BacktestRunner::getExplorerTask).toList();
	}
	
	@Transactional
	public void runTask(String taskName) {
		if(!runnerMap.containsKey(taskName)) {
			throw new IllegalArgumentException(String.format("不存在探索任务[%s]", taskName));
		}
		log.info("执行策略探索任务：{}", taskName);
		runnerMap.get(taskName).execute();
	}
	
	public List<BacktestModuleVO> getTaskDetails(String taskName){
		if(!runnerMap.containsKey(taskName)) {
			throw new IllegalArgumentException(String.format("不存在探索任务[%s]", taskName));
		}
		return runnerMap.get(taskName).getBacktestModule();
	}
	
	public BacktestModuleTradeDetailsVO getModuleTradeDetails(String moduleName) {
		if(!moduleRunnerMap.containsKey(moduleName)) {
			throw new IllegalArgumentException(String.format("不存在测试模组[%s]", moduleName));
		}
		return moduleRunnerMap.get(moduleName).getBacktestModuleTradeDetails(moduleName);
	}

	@Override
	public void run(String... args) throws Exception {
		StreamSupport.stream(repo.findAll().spliterator(), false).forEach(taskDO -> {
			ExplorerTaskDescription task = taskDO.convertTo();
			try {				
				BacktestRunner runner = factory.createTestRunner(task);
				runnerMap.put(task.getTaskName(), runner);
				runner.getModules().forEach(m -> moduleRunnerMap.put(m.getModuleContext().getModuleDescription().getName(), runner));
			} catch (Exception e) {
				log.error("加载回测任务异常", e);
				removeTask(task.getTaskName());
				log.warn("自动移除 {}", task.getTaskName());
			}
		});
		
	}
	
}
