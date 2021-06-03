package tech.xuanwu.northstar.strategy.cta.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;

import tech.xuanwu.northstar.strategy.common.DynamicParams;
import tech.xuanwu.northstar.strategy.common.SignalPolicy;
import tech.xuanwu.northstar.strategy.cta.model.CtaStrategyModule;
import tech.xuanwu.northstar.strategy.cta.persistence.StrategyModuleRepository;

public class CtaModuleService implements InitializingBean{
	
	private ApplicationContext ctx;
	
	private StrategyModuleRepository moduleRepo;
	
	public CtaModuleService(ApplicationContext ctx, StrategyModuleRepository moduleRepo) {
		this.ctx = ctx;
		this.moduleRepo = moduleRepo;
	}
	
	/**
	 * 查询可选的信号策略
	 * @return
	 */
	public List<String> getRegisteredSignalPolicies(){
		Map<String, SignalPolicy> policies = ctx.getBeansOfType(SignalPolicy.class);
		return policies.values().stream()
				.map(i -> i.name())
				.collect(Collectors.toList());
	}
	
	/**
	 * 查询可选的交易策略
	 * @return
	 */
	public List<String> getRegisteredDealers(){
		return null;
	}
	
	/**
	 * 查询可选的风控策略
	 * @return
	 */
	public List<String> getRegisteredRiskControlPolicies(){
		return null;
	}
	
	/**
	 * 获取组件参数
	 * @param name
	 * @return
	 */
	public DynamicParams<?> getComponentParams(String name){
		SignalPolicy policy = (SignalPolicy) ctx.getBean(name);
		DynamicParams<?> params = policy.getDynamicParams();
		params.metaToSource();
		return params;
	}

	/**
	 * 新增模组
	 * @param module
	 * @param shouldSave
	 */
	public void createModule(CtaStrategyModule module, boolean shouldSave) {
		
		if(shouldSave) {
			moduleRepo.save(module);
		}
	}
	
	/**
	 * 更新模组
	 * @param module
	 */
	public void updateModule(CtaStrategyModule module) {
		moduleRepo.save(module);
	}
	
	/**
	 * 查询所有模组
	 * @return
	 */
	public List<CtaStrategyModule> getCurrentModules(){
		return moduleRepo.findAll();
	}
	
	/**
	 * 移除模组
	 * @param moduleName
	 */
	public void removeModule(String moduleName) {
		moduleRepo.deleteById(moduleName);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// 加载已有模组
	}
}
