package org.dromara.northstar.app.restful;

import java.util.List;

import org.dromara.northstar.app.service.StrategyExplorerService;
import org.dromara.northstar.common.exception.ForbiddenInDemoException;
import org.dromara.northstar.common.model.BacktestModuleTradeDetailsVO;
import org.dromara.northstar.common.model.BacktestModuleVO;
import org.dromara.northstar.common.model.ExplorerTaskDescription;
import org.dromara.northstar.common.model.ExplorerTaskVO;
import org.dromara.northstar.common.utils.MessageSourceHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("northstar/explorer")
@RestController
public class StrategyExplorerController {
	
	@Autowired
	private StrategyExplorerService service;
	
	@Autowired
	private Environment env;

	@PostMapping("task")
	public List<ExplorerTaskVO> createTask(@RequestBody ExplorerTaskDescription task){
		if(env != null && env.matchesProfiles("demo")) {
			throw new ForbiddenInDemoException();
		}
		Assert.hasText(task.getTaskName(), () -> MessageSourceHolder.getMessageSource().getMessage("explorer.error.taskNameNotEmpty", null, LocaleContextHolder.getLocale()));
		Assert.hasText(task.getStartDate(), () -> MessageSourceHolder.getMessageSource().getMessage("explorer.error.startDateNotEmpty", null, LocaleContextHolder.getLocale()));
		Assert.hasText(task.getEndDate(), () -> MessageSourceHolder.getMessageSource().getMessage("explorer.error.endDateNotEmpty", null, LocaleContextHolder.getLocale()));
		Assert.hasText(task.getPreloadFrom(), () -> MessageSourceHolder.getMessageSource().getMessage("explorer.error.preloadDateNotEmpty", null, LocaleContextHolder.getLocale()));
		Assert.notEmpty(task.getContracts(), () -> MessageSourceHolder.getMessageSource().getMessage("explorer.error.contractsNotEmpty", null, LocaleContextHolder.getLocale()));
		Assert.notNull(task.getStrategySetting(), MessageSourceHolder.getMessageSource().getMessage("explorer.error.strategyNotNull", null, LocaleContextHolder.getLocale()));
		return service.createTask(task);
	}
	
	@DeleteMapping("task")
	public List<ExplorerTaskVO> removeTask(String taskName){
		return service.removeTask(taskName);
	}
	
	@GetMapping("task")
	public List<ExplorerTaskVO> getTasks(){
		return service.getTasks();
	}
	
	@PutMapping("task")
	public void runTask(String taskName) {
		service.runTask(taskName);
	}
	
	@GetMapping("task/details")
	public List<BacktestModuleVO> getTaskDetails(String taskName){
		return service.getTaskDetails(taskName);
	}
	
	@GetMapping("trades")
	public BacktestModuleTradeDetailsVO getModuleTradeDetails(String moduleName) {
		return service.getModuleTradeDetails(moduleName);
	}
	
}
