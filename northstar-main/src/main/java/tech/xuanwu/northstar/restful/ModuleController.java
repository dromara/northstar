package tech.xuanwu.northstar.restful;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import tech.xuanwu.northstar.common.model.ResultBean;
import tech.xuanwu.northstar.service.ModuleService;
import tech.xuanwu.northstar.strategy.common.model.ModuleInfo;
import tech.xuanwu.northstar.strategy.common.model.ModulePerformance;
import tech.xuanwu.northstar.strategy.common.model.meta.ComponentField;
import tech.xuanwu.northstar.strategy.common.model.meta.ComponentMetaInfo;

@RestController
public class ModuleController {
	
	@Autowired
	private ModuleService service;
	
	@GetMapping("/signal/policies")
	public ResultBean<List<ComponentMetaInfo>> getRegisteredSignalPolicies(){
		
		return new ResultBean<>(service.getRegisteredSignalPolicies());
	}
	
	@GetMapping("/riskControl/rules")
	public ResultBean<List<ComponentMetaInfo>> getRegisteredRiskControlRules(){
		return new ResultBean<>(service.getRegisteredRiskControlRules());
	}
	
	@GetMapping("/trade/dealers")
	public ResultBean<List<ComponentMetaInfo>> getRegisteredDealers(){
		return new ResultBean<>(service.getRegisteredDealers());
	}
	
	@PostMapping("/component/params")
	public ResultBean<Map<String, ComponentField>> getComponentParams(@RequestBody ComponentMetaInfo info) throws ClassNotFoundException{
		Assert.notNull(info, "组件不能为空");
		Assert.notNull(info.getClassName(), "组件类信息不能为空");
		return new ResultBean<>(service.getComponentParams(info));
	}
	
	@PostMapping("/module")
	public ResultBean<Boolean> createModule(@RequestBody ModuleInfo module) throws Exception{
		Assert.notNull(module, "模组信息不能为空");
		return new ResultBean<>(service.createModule(module));
	}
	
	@PutMapping("/module")
	public ResultBean<Boolean> updateModule(@RequestBody ModuleInfo module) throws Exception{
		Assert.notNull(module, "模组信息不能为空");
		return new ResultBean<>(service.updateModule(module));
	}
	
	@GetMapping("/module")
	public ResultBean<List<ModuleInfo>> getAllModules(){
		return new ResultBean<>(service.getCurrentModuleInfos());
	}
	
	@DeleteMapping("/module")
	public ResultBean<Void> removeModule(String name){
		Assert.hasText(name, "模组名称不能为空");
		service.removeModule(name);
		return new ResultBean<>(null);
	}
	
	@GetMapping("/module/perf")
	public ResultBean<ModulePerformance> getModulePerformance(String name){
		Assert.hasText(name, "模组名称不能为空");
		return new ResultBean<>(service.getModulePerformance(name));
	}
	
	@GetMapping("/module/toggle")
	public ResultBean<Void> toggleModuleState(String name){
		Assert.hasText(name, "模组名称不能为空");
		service.toggleState(name);
		return new ResultBean<>(null);
	}
	
	
}
