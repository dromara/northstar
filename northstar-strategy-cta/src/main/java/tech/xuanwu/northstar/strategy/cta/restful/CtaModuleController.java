package tech.xuanwu.northstar.strategy.cta.restful;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tech.xuanwu.northstar.common.model.ResultBean;
import tech.xuanwu.northstar.strategy.common.DynamicParams;
import tech.xuanwu.northstar.strategy.common.SignalPolicy;
import tech.xuanwu.northstar.strategy.cta.model.CtaStrategyModule;
import tech.xuanwu.northstar.strategy.cta.service.CtaModuleService;

@RestController
@RequestMapping("/cta")
public class CtaModuleController {
	
	@Autowired
	private CtaModuleService service;
	
	@GetMapping("/signal/policies")
	public ResultBean<List<String>> getRegisteredSignalPolicies(){
		
		return new ResultBean<>(service.getRegisteredSignalPolicies());
	}
	
	@GetMapping("/component/params")
	public ResultBean<DynamicParams<?>> getComponentParams(String name){
		Assert.hasText(name, "组件名称不能为空");
		return new ResultBean<>(service.getComponentParams(name));
	}
	
	@PostMapping("/module")
	public ResultBean<Void> createModule(@RequestBody CtaStrategyModule module){
		Assert.notNull(module, "模组信息不能为空");
		service.createModule(module, true);
		return new ResultBean<>(null);
	}
	
	@PutMapping("/module")
	public ResultBean<Void> updateModule(@RequestBody CtaStrategyModule module){
		Assert.notNull(module, "模组信息不能为空");
		service.updateModule(module);
		return new ResultBean<>(null);
	}
	
	@GetMapping("/module")
	public ResultBean<List<CtaStrategyModule>> getAllModules(){
		return new ResultBean<>(service.getCurrentModules());
	}
	
	@DeleteMapping("/module")
	public ResultBean<Void> removeModule(String name){
		Assert.hasText(name, "模组名称不能为空");
		service.removeModule(name);
		return new ResultBean<>(null);
	}
}
