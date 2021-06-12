package tech.xuanwu.northstar.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;

import tech.xuanwu.northstar.persistence.ModuleRepository;
import tech.xuanwu.northstar.strategy.common.Dealer;
import tech.xuanwu.northstar.strategy.common.DynamicParamsAware;
import tech.xuanwu.northstar.strategy.common.RiskControlRule;
import tech.xuanwu.northstar.strategy.common.SignalPolicy;
import tech.xuanwu.northstar.strategy.common.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.common.model.ModuleInfo;
import tech.xuanwu.northstar.strategy.common.model.ModuleStatus;
import tech.xuanwu.northstar.strategy.common.model.meta.ComponentField;
import tech.xuanwu.northstar.strategy.common.model.meta.ComponentMetaInfo;
import tech.xuanwu.northstar.strategy.common.model.meta.DynamicParams;

public class CtaModuleService implements InitializingBean{
	
	private ApplicationContext ctx;
	
	private ModuleRepository moduleRepo;
	
	public CtaModuleService(ApplicationContext ctx, ModuleRepository moduleRepo) {
		this.ctx = ctx;
		this.moduleRepo = moduleRepo;
	}
	
	/**
	 * 查询可选的信号策略
	 * @return
	 */
	public List<ComponentMetaInfo> getRegisteredSignalPolicies(){
		return getComponentMeta(SignalPolicy.class);
	}
	
	/**
	 * 查询可选的风控规则
	 * @return
	 */
	public List<ComponentMetaInfo> getRegisteredRiskControlRules(){
		return getComponentMeta(RiskControlRule.class);
	}
	
	/**
	 * 查询可选的交易策略
	 * @return
	 */
	public List<ComponentMetaInfo> getRegisteredDealers(){
		return getComponentMeta(Dealer.class);
	}
	
	private List<ComponentMetaInfo> getComponentMeta(Class<?> clz){
		Map<String, Object> objMap = ctx.getBeansWithAnnotation(StrategicComponent.class);
		List<ComponentMetaInfo> result = new ArrayList<>(objMap.size());
		for(Entry<String, Object> e : objMap.entrySet()) {
			if(clz.isAssignableFrom(e.getValue().getClass())) {
				StrategicComponent anno = e.getValue().getClass().getAnnotation(StrategicComponent.class);
				result.add(new ComponentMetaInfo(anno.value(), e.getValue().getClass().getName()));
			}
		}
		return result;
	}
	
	/**
	 * 获取组件参数
	 * @param name
	 * @return
	 * @throws ClassNotFoundException 
	 */
	public Map<String, ComponentField> getComponentParams(ComponentMetaInfo info) throws ClassNotFoundException{
		String className = info.getClassName();
		Class<?> clz = Class.forName(className);
		DynamicParamsAware aware = (DynamicParamsAware) ctx.getBean(clz);
		DynamicParams params = aware.getDynamicParams();
		return params.getMetaInfo();
	}

	/**
	 * 新增模组
	 * @param module
	 * @param shouldSave
	 */
	public void createModule(ModuleInfo module) {
		
		moduleRepo.saveModuleInfo(module);
	}
	
	/**
	 * 加载模组
	 * @param module
	 * @param status
	 */
	public void loadModule(ModuleInfo module, ModuleStatus status) {
		
		
	}
	
	/**
	 * 查询所有模组
	 * @return
	 */
	public List<ModuleInfo> getCurrentModules(){
		return moduleRepo.findAllModuleInfo();
	}
	
	/**
	 * 移除模组
	 * @param moduleName
	 */
	public void removeModule(String moduleName) {
		moduleRepo.deleteModuleInfoById(moduleName);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Map<String, ModuleStatus> statusMap = moduleRepo.loadModuleStatus();
		for(ModuleInfo m : getCurrentModules()) {
			ModuleStatus status = statusMap.get(m.getModuleName());
			if(status == null) {
				throw new IllegalStateException("找不到模组的状态信息");
			}
			loadModule(m, status);
		}
	}
	
}
