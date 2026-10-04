package org.dromara.northstar.app.restful;

import java.util.List;
import java.util.Map;

import org.dromara.northstar.app.rl.RLTrainingService;
import org.dromara.northstar.common.model.rl.RLModelVO;
import org.dromara.northstar.common.model.rl.RLStrategyVO;
import org.dromara.northstar.common.model.rl.RLTrainingTaskDTO;
import org.dromara.northstar.common.model.rl.RLTrainingTaskDetailVO;
import org.dromara.northstar.common.model.rl.RLTrainingTaskVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RL 强化学习训练接口（设计文档 §6.3）。
 */
@RestController
@RequestMapping("northstar/rl")
public class RLController {

	@Autowired
	private RLTrainingService service;

	/** RL 策略列表（仅 RLStrategy 实现类） */
	@GetMapping("strategies")
	public List<RLStrategyVO> rlStrategies() {
		return service.rlStrategies();
	}

	/** 模型列表（含 hyperParamFields、versionDetails、network） */
	@GetMapping("models")
	public List<RLModelVO> models() {
		return service.models();
	}

	/** 创建训练任务（仅参数校验与版本 fork，不加载数据、不启动训练） */
	@PostMapping("training")
	public RLTrainingTaskVO createTask(@RequestBody RLTrainingTaskDTO dto) {
		return service.createTask(dto);
	}

	/** 任务列表 */
	@GetMapping("training")
	public List<RLTrainingTaskVO> listTasks() {
		return service.listTasks();
	}

	/** 任务详情 + 每轮指标 */
	@GetMapping("training/{taskId}")
	public RLTrainingTaskDetailVO detail(@PathVariable String taskId) {
		return service.detail(taskId);
	}

	/** 动态调整超参数（下一轮生效） */
	@PutMapping("training/{taskId}/params")
	public RLTrainingTaskVO updateParams(@PathVariable String taskId, @RequestBody Map<String, Double> params) {
		return service.updateParams(taskId, params);
	}

	@GetMapping("training/{taskId}/load-data")
	public RLTrainingTaskVO loadData(@PathVariable String taskId) {
		return service.loadData(taskId);
	}

	@GetMapping("training/{taskId}/start")
	public RLTrainingTaskVO start(@PathVariable String taskId) {
		return service.start(taskId);
	}

	@GetMapping("training/{taskId}/pause")
	public RLTrainingTaskVO pause(@PathVariable String taskId) {
		return service.pause(taskId);
	}

	@GetMapping("training/{taskId}/resume")
	public RLTrainingTaskVO resume(@PathVariable String taskId) {
		return service.resume(taskId);
	}

	@GetMapping("training/{taskId}/stop")
	public RLTrainingTaskVO stop(@PathVariable String taskId) {
		return service.stop(taskId);
	}

	/** 删除任务（运行中/已暂停需先停止；加载中任务协作式取消；未开始训练的任务会同时回收版本与数据文件） */
	@DeleteMapping("training/{taskId}")
	public void deleteTask(@PathVariable String taskId) {
		service.deleteTask(taskId);
	}
}
