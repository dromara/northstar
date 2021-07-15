package tech.xuanwu.northstar.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;

import tech.xuanwu.northstar.common.constant.DateTimeConstant;
import tech.xuanwu.northstar.common.model.ContractManager;
import tech.xuanwu.northstar.domain.GatewayConnection;
import tech.xuanwu.northstar.gateway.api.Gateway;
import tech.xuanwu.northstar.gateway.api.TradeGateway;
import tech.xuanwu.northstar.manager.GatewayAndConnectionManager;
import tech.xuanwu.northstar.manager.ModuleManager;
import tech.xuanwu.northstar.persistence.MarketDataRepository;
import tech.xuanwu.northstar.persistence.ModuleRepository;
import tech.xuanwu.northstar.persistence.po.MinBarDataPO;
import tech.xuanwu.northstar.strategy.common.AbstractModuleFactory;
import tech.xuanwu.northstar.strategy.common.Dealer;
import tech.xuanwu.northstar.strategy.common.DynamicParamsAware;
import tech.xuanwu.northstar.strategy.common.ModulePosition;
import tech.xuanwu.northstar.strategy.common.ModuleTrade;
import tech.xuanwu.northstar.strategy.common.RiskControlRule;
import tech.xuanwu.northstar.strategy.common.RiskController;
import tech.xuanwu.northstar.strategy.common.SignalPolicy;
import tech.xuanwu.northstar.strategy.common.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.common.constants.ModuleState;
import tech.xuanwu.northstar.strategy.common.constants.ModuleType;
import tech.xuanwu.northstar.strategy.common.model.GenericRiskController;
import tech.xuanwu.northstar.strategy.common.model.ModuleAgent;
import tech.xuanwu.northstar.strategy.common.model.ModuleInfo;
import tech.xuanwu.northstar.strategy.common.model.ModulePerformance;
import tech.xuanwu.northstar.strategy.common.model.ModuleStatus;
import tech.xuanwu.northstar.strategy.common.model.StrategyModule;
import tech.xuanwu.northstar.strategy.common.model.data.BarData;
import tech.xuanwu.northstar.strategy.common.model.meta.ComponentAndParamsPair;
import tech.xuanwu.northstar.strategy.common.model.meta.ComponentField;
import tech.xuanwu.northstar.strategy.common.model.meta.ComponentMetaInfo;
import tech.xuanwu.northstar.strategy.common.model.meta.DynamicParams;
import tech.xuanwu.northstar.strategy.cta.CtaModuleFactory;
import tech.xuanwu.northstar.utils.ProtoBeanUtils;
import xyz.redtorch.pb.CoreField.BarField;

public class ModuleService implements InitializingBean{
	
	private ApplicationContext ctx;
	
	private ModuleRepository moduleRepo;
	
	private MarketDataRepository mdRepo;
	
	private ModuleManager mdlMgr;
	
	private GatewayAndConnectionManager gatewayConnMgr;
	
	private ContractManager contractMgr;
	
	public ModuleService(ApplicationContext ctx, ModuleRepository moduleRepo, MarketDataRepository mdRepo,
			ModuleManager mdlMgr, GatewayAndConnectionManager gatewayConnMgr, ContractManager contractMgr) {
		this.ctx = ctx;
		this.moduleRepo = moduleRepo;
		this.mdRepo = mdRepo;
		this.mdlMgr = mdlMgr;
		this.gatewayConnMgr = gatewayConnMgr;
		this.contractMgr = contractMgr;
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
	 * @throws Exception 
	 */
	public void createModule(ModuleInfo info) throws Exception {
		loadModule(info, null);
		moduleRepo.saveModuleInfo(info);
	}
	
	/**
	 * 加载模组
	 * @param module
	 * @param status
	 */
	private void loadModule(ModuleInfo info, ModuleStatus status) throws Exception {
		SignalPolicy signalPolicy =  resolveComponent(info.getSignalPolicy(), SignalPolicy.class);
		Dealer dealer = resolveComponent(info.getDealer(), Dealer.class);
		List<RiskControlRule> riskRules = new ArrayList<>();
		for(ComponentAndParamsPair pair : info.getRiskControlRules()) {
			riskRules.add(resolveComponent(pair, RiskControlRule.class));
		}
		
		String gatewayId = info.getAccountGatewayId();
		GatewayConnection conn = gatewayConnMgr.getGatewayConnectionById(gatewayId);
		Gateway gateway = gatewayConnMgr.getGatewayById(gatewayId);
		String mktGatewayId = conn.getGwDescription().getBindedMktGatewayId();
		
		AbstractModuleFactory factory = null;
		RiskController riskController = null;
		if(info.getType() == ModuleType.CTA) {
			factory = new CtaModuleFactory();
			riskController = new GenericRiskController(riskRules);
		} else {
			// TODO 不同的策略模式采用不同的工厂实现
		}
		
		ModulePosition mPosition = status == null ? factory.newModulePosition() : factory.loadModulePosition(status);
		ModuleTrade mTrade = status == null ? factory.newModuleTrade() : factory.loadModuleTrade(moduleRepo.findTradeDescription(info.getModuleName()));
		
		ModuleState state = status == null ? ModuleState.EMPTY : status.getState();
		Map<String, BarData> barDataMap = new HashMap<>();
		for(String unifiedSymbol : signalPolicy.bindedUnifiedSymbols()) {
			int daysOfRefData = info.getDaysOfRefData();
			LocalDateTime now = LocalDateTime.now();
			LocalDateTime currentTradeDay = now.plusHours(now.getDayOfWeek().getValue() == 5 ? 54 : 6);
			List<BarField> refBarList = new ArrayList<>(daysOfRefData * 500);
			for(int i=daysOfRefData; i>=0; i--) {
				String dayStr = currentTradeDay.minusDays(i).format(DateTimeConstant.D_FORMAT_INT_FORMATTER);
				List<MinBarDataPO> dataBarPOList = mdRepo.loadDataByDate(mktGatewayId, unifiedSymbol, dayStr);
				for(MinBarDataPO po : dataBarPOList) {
					BarField.Builder bb = BarField.newBuilder();
					ProtoBeanUtils.toProtoBean(bb, po);
					refBarList.add(bb.build());
				}
			}
			barDataMap.put(unifiedSymbol, new BarData(unifiedSymbol, refBarList));
		}
		
		ModuleAgent agent = ModuleAgent.builder()
				.name(info.getModuleName())
				.accountGatewayId(info.getAccountGatewayId())
				.gateway((TradeGateway)gateway)
				.enabled(info.isEnabled())
				.state(state)
				.modulePosition(mPosition)
				.build();
		
		signalPolicy.setRefBarData(barDataMap);
		dealer.setContractManager(contractMgr);
		StrategyModule module = new StrategyModule(agent, signalPolicy, riskController, dealer, mPosition, mTrade);
		mdlMgr.addModule(module);
		moduleRepo.saveModuleStatus(module.getModuleStatus());
	}
	
	/**
	 * 查询所有模组
	 * @return
	 */
	public List<ModuleInfo> getCurrentModuleInfos(){
		return moduleRepo.findAllModuleInfo();
	}
	
	/**
	 * 获取模组绩效
	 * @param moduleName
	 * @return
	 */
	public ModulePerformance getModulePerformance(String moduleName) {
		return mdlMgr.getModulePerformance(moduleName);
	}
	
	/**
	 * 移除模组
	 * @param moduleName
	 */
	public void removeModule(String moduleName) {
		mdlMgr.removeModule(moduleName);
		moduleRepo.deleteModuleInfoById(moduleName);
		moduleRepo.removeModuleStatus(moduleName);
		moduleRepo.removeTradeDescription(moduleName);
	}
	
	
	private <T extends DynamicParamsAware> T resolveComponent(ComponentAndParamsPair metaInfo, Class<T> clz) throws Exception {
		Map<String, ComponentField> fieldMap = new HashMap<>();
		for(ComponentField cf : metaInfo.getInitParams()) {
			fieldMap.put(cf.getName(), cf);
		}
		String clzName = metaInfo.getComponentMeta().getClassName();
		String paramClzName = clzName + "$InitParams";
		Class<?> type = Class.forName(clzName);
		Class<?> paramType = Class.forName(paramClzName);
		DynamicParamsAware obj = (DynamicParamsAware) type.getDeclaredConstructor().newInstance();
		DynamicParams paramObj = (DynamicParams) paramType.getDeclaredConstructor().newInstance();
		paramObj.resolveFromSource(fieldMap);
		obj.initWithParams(paramObj);
		return (T) obj;
	}
	
	/**
	 * 切换模组状态
	 */
	public void toggleState(String moduleName) {
		mdlMgr.toggleState(moduleName);
		ModuleInfo info = moduleRepo.findModuleInfo(moduleName);
		info.setEnabled(!info.isEnabled());
		moduleRepo.saveModuleInfo(info);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Map<String, ModuleStatus> statusMap = moduleRepo.loadModuleStatus();
		for(ModuleInfo m : getCurrentModuleInfos()) {
			ModuleStatus status = statusMap.get(m.getModuleName());
			if(status == null) {
				throw new IllegalStateException("找不到模组的状态信息");
			}
			loadModule(m, status);
		}
	}
	
}
