package tech.xuanwu.northstar.main.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;

import tech.xuanwu.northstar.common.model.ContractManager;
import tech.xuanwu.northstar.domain.GatewayConnection;
import tech.xuanwu.northstar.gateway.api.Gateway;
import tech.xuanwu.northstar.gateway.api.TradeGateway;
import tech.xuanwu.northstar.main.manager.GatewayAndConnectionManager;
import tech.xuanwu.northstar.main.manager.ModuleManager;
import tech.xuanwu.northstar.main.persistence.MarketDataRepository;
import tech.xuanwu.northstar.main.persistence.ModuleRepository;
import tech.xuanwu.northstar.main.persistence.po.MinBarDataPO;
import tech.xuanwu.northstar.main.utils.ProtoBeanUtils;
import tech.xuanwu.northstar.strategy.common.Dealer;
import tech.xuanwu.northstar.strategy.common.DynamicParamsAware;
import tech.xuanwu.northstar.strategy.common.RiskControlRule;
import tech.xuanwu.northstar.strategy.common.RiskController;
import tech.xuanwu.northstar.strategy.common.SignalPolicy;
import tech.xuanwu.northstar.strategy.common.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.common.model.GenericRiskController;
import tech.xuanwu.northstar.strategy.common.model.ModuleInfo;
import tech.xuanwu.northstar.strategy.common.model.ModulePosition;
import tech.xuanwu.northstar.strategy.common.model.ModuleStatus;
import tech.xuanwu.northstar.strategy.common.model.StrategyModule;
import tech.xuanwu.northstar.strategy.common.model.data.BarData;
import tech.xuanwu.northstar.strategy.common.model.entity.ModuleDataRef;
import tech.xuanwu.northstar.strategy.common.model.entity.ModuleDealRecord;
import tech.xuanwu.northstar.strategy.common.model.entity.ModuleRealTimeInfo;
import tech.xuanwu.northstar.strategy.common.model.entity.ModuleTradeRecord;
import tech.xuanwu.northstar.strategy.common.model.meta.ComponentAndParamsPair;
import tech.xuanwu.northstar.strategy.common.model.meta.ComponentField;
import tech.xuanwu.northstar.strategy.common.model.meta.ComponentMetaInfo;
import tech.xuanwu.northstar.strategy.common.model.meta.DynamicParams;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
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
	public boolean createModule(ModuleInfo info) throws Exception {
		loadModule(info, new ModuleStatus(info.getModuleName()));
		return moduleRepo.saveModuleInfo(info);
	}
	
	/**
	 * 更新模组
	 * @param info
	 * @throws Exception 
	 */
	public boolean updateModule(ModuleInfo info) throws Exception {
		mdlMgr.removeModule(info.getModuleName());
		ModuleStatus status = moduleRepo.loadModuleStatus(info.getModuleName());
		if(status == null) {
			status = new ModuleStatus(info.getModuleName());
		}
		
		moduleRepo.deleteModuleInfoById(info.getModuleName());
		loadModule(info, status);
		return moduleRepo.saveModuleInfo(info);
	}
	
	/**
	 * 加载模组
	 * @param module
	 * @param status
	 */
	private void loadModule(ModuleInfo info, ModuleStatus moduleStatus) throws Exception {
		SignalPolicy signalPolicy =  resolveComponent(info.getSignalPolicy());
		Dealer dealer = resolveComponent(info.getDealer());
		List<RiskControlRule> riskRules = new ArrayList<>();
		for(ComponentAndParamsPair pair : info.getRiskControlRules()) {
			riskRules.add(resolveComponent(pair));
		}
		
		String gatewayId = info.getAccountGatewayId();
		GatewayConnection conn = gatewayConnMgr.getGatewayConnectionById(gatewayId);
		Gateway gateway = gatewayConnMgr.getGatewayById(gatewayId);
		String mktGatewayId = conn.getGwDescription().getBindedMktGatewayId();
		
		RiskController riskController = new GenericRiskController(riskRules);
		int refLength = signalPolicy.getBarDataMaxRefLength();
		LinkedList<BarField> barList = new LinkedList<>();
		
		for(String unifiedSymbol : signalPolicy.bindedUnifiedSymbols()) {
			List<String> availableDates = mdRepo.findDataAvailableDates(mktGatewayId, unifiedSymbol, false);
			for(String date : availableDates) {
				List<MinBarDataPO> dataBarPOList = mdRepo.loadDataByDate(mktGatewayId, unifiedSymbol, date);
				for(int i=dataBarPOList.size() - 1; i > -1; i--) {
					MinBarDataPO po = dataBarPOList.get(i);
					BarField.Builder bb = BarField.newBuilder();
					ProtoBeanUtils.toProtoBean(bb, po);
					barList.addFirst(bb.build());
					if(barList.size() >= refLength) {
						break;
					}
				}
				if(barList.size() >= refLength) {
					break;
				}
			}
			signalPolicy.setBarData(new BarData(unifiedSymbol, barList));
		}
		
		signalPolicy.setModuleStatus(moduleStatus);
		dealer.setContractManager(contractMgr);
		StrategyModule module = StrategyModule.builder()
				.gateway((TradeGateway)gateway)
				.mktGatewayId(mktGatewayId)
				.status(moduleStatus)
				.disabled(!info.isEnabled())
				.dealer(dealer)
				.signalPolicy(signalPolicy)
				.riskController(riskController)
				.contractMgr(contractMgr)
				.build();
		mdlMgr.addModule(module);
	}
	
	/**
	 * 查询所有模组
	 * @return
	 */
	public List<ModuleInfo> getCurrentModuleInfos(){
		return moduleRepo.findAllModuleInfo();
	}
	
	/**
	 * 获取模组实时信息
	 * @param moduleName
	 * @return
	 */
	public ModuleRealTimeInfo getModuleRealTimeInfo(String moduleName) {
		return mdlMgr.getModule(moduleName).getRealTimeInfo();
	}
	
	/**
	 * 获取模组引用数据
	 * @param moduleName
	 * @return
	 */
	public ModuleDataRef getModuleDataRef(String moduleName) {
		return mdlMgr.getModule(moduleName).getDataRef();
	}
	
	/**
	 * 获取模组交易历史
	 * @param moduleName
	 * @return
	 */
	public List<ModuleDealRecord> getDealRecords(String moduleName) {
		return moduleRepo.findDealRecords(moduleName);
	}
	
	/**
	 * 获取模组成交历史
	 * @param moduleName
	 * @return
	 */
	public List<ModuleTradeRecord> getTradeRecords(String moduleName){
		return moduleRepo.findTradeRecords(moduleName);
	}
	
	/**
	 * 移除模组
	 * @param moduleName
	 */
	public void removeModule(String moduleName) {
		mdlMgr.removeModule(moduleName);
		moduleRepo.deleteModuleInfoById(moduleName);
		moduleRepo.removeModuleStatus(moduleName);
		moduleRepo.removeDealRecords(moduleName);
		moduleRepo.removeTradeRecords(moduleName);
	}
	
	
	private <T extends DynamicParamsAware> T resolveComponent(ComponentAndParamsPair metaInfo) throws Exception {
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
	public boolean toggleState(String moduleName) {
		mdlMgr.toggleState(moduleName);
		ModuleInfo info = moduleRepo.findModuleInfo(moduleName);
		info.setEnabled(!info.isEnabled());
		moduleRepo.saveModuleInfo(info);
		return true;
	}
	
	public boolean updatePosition(String moduleName, ModulePosition position) {
		ModuleStatus status = mdlMgr.getModule(moduleName).updatePosition(position);
		moduleRepo.saveModuleStatus(status);
		return true;
	}
	
	public boolean removePosition(String moduleName, String unifiedSymbol, PositionDirectionEnum dir) {
		ModuleStatus status = mdlMgr.getModule(moduleName).removePosition(unifiedSymbol, dir);
		moduleRepo.saveModuleStatus(status);
		return true;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		for(ModuleInfo m : getCurrentModuleInfos()) {
			ModuleStatus status = moduleRepo.loadModuleStatus(m.getModuleName());
			if(status == null) {
				status = new ModuleStatus(m.getModuleName());
			}
			loadModule(m, status);
		}
	}
	
}
