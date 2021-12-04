package tech.xuanwu.northstar.main.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;

import com.google.common.collect.Lists;

import tech.xuanwu.northstar.common.constant.DateTimeConstant;
import tech.xuanwu.northstar.common.model.ContractManager;
import tech.xuanwu.northstar.domain.GatewayAndConnectionManager;
import tech.xuanwu.northstar.domain.GatewayConnection;
import tech.xuanwu.northstar.domain.strategy.ModuleManager;
import tech.xuanwu.northstar.domain.strategy.ModulePosition;
import tech.xuanwu.northstar.domain.strategy.ModuleStatus;
import tech.xuanwu.northstar.domain.strategy.StrategyModule;
import tech.xuanwu.northstar.domain.strategy.StrategyModuleFactory;
import tech.xuanwu.northstar.main.persistence.MarketDataRepository;
import tech.xuanwu.northstar.main.persistence.ModuleRepository;
import tech.xuanwu.northstar.main.persistence.po.MinBarDataPO;
import tech.xuanwu.northstar.main.persistence.po.ModulePositionPO;
import tech.xuanwu.northstar.main.utils.ProtoBeanUtils;
import tech.xuanwu.northstar.strategy.api.DealerPolicy;
import tech.xuanwu.northstar.strategy.api.DynamicParamsAware;
import tech.xuanwu.northstar.strategy.api.RiskControlRule;
import tech.xuanwu.northstar.strategy.api.SignalPolicy;
import tech.xuanwu.northstar.strategy.api.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.api.model.ComponentField;
import tech.xuanwu.northstar.strategy.api.model.ComponentMetaInfo;
import tech.xuanwu.northstar.strategy.api.model.DynamicParams;
import tech.xuanwu.northstar.strategy.api.model.ModuleDealRecord;
import tech.xuanwu.northstar.strategy.api.model.ModuleInfo;
import tech.xuanwu.northstar.strategy.api.model.ModulePositionInfo;
import tech.xuanwu.northstar.strategy.api.model.ModuleRealTimeInfo;
import tech.xuanwu.northstar.strategy.api.model.ModuleTradeRecord;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;

public class ModuleService implements InitializingBean{
	
	private ApplicationContext ctx;
	
	private ModuleRepository moduleRepo;
	
	private MarketDataRepository mdRepo;
	
	private ModuleManager mdlMgr;
	
	private GatewayAndConnectionManager gatewayConnMgr;
	
	private ContractManager contractMgr;
	
	private StrategyModuleFactory moduleFactory;
	
	public ModuleService(ApplicationContext ctx, ModuleRepository moduleRepo, MarketDataRepository mdRepo,
			ModuleManager mdlMgr, GatewayAndConnectionManager gatewayConnMgr, ContractManager contractMgr) {
		this.ctx = ctx;
		this.moduleRepo = moduleRepo;
		this.mdRepo = mdRepo;
		this.mdlMgr = mdlMgr;
		this.gatewayConnMgr = gatewayConnMgr;
		this.contractMgr = contractMgr;
		this.moduleFactory = new StrategyModuleFactory(gatewayConnMgr, contractMgr);
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
		return getComponentMeta(DealerPolicy.class);
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
		loadModule(info, Collections.emptyList());
		return moduleRepo.saveModuleInfo(info);
	}
	
	/**
	 * 更新模组
	 * @param info
	 * @throws Exception 
	 */
	public boolean updateModule(ModuleInfo info) throws Exception {
		mdlMgr.removeModule(info.getModuleName());
		ModulePositionPO po = moduleRepo.loadModulePosition(info.getModuleName());
		moduleRepo.deleteModuleInfoById(info.getModuleName());
		loadModule(info, po.getPositions());
		return moduleRepo.saveModuleInfo(info);
	}
	
	/**
	 * 加载模组
	 * @param module
	 * @param status
	 */
	private void loadModule(ModuleInfo info, List<ModulePositionInfo> positionInfos) throws Exception {
		StrategyModule strategyModule = moduleFactory.makeModule(info, positionInfos);
		Set<String> interestSymbols = strategyModule.bindedContractUnifiedSymbols();
//		List<BarData> barDataList = new ArrayList<>();
//		LinkedList<BarField> barList = new LinkedList<>();
//		
//		String gatewayId = info.getAccountGatewayId();
//		GatewayConnection conn = gatewayConnMgr.getGatewayConnectionById(gatewayId);
//		String mktGatewayId = conn.getGwDescription().getBindedMktGatewayId();
//		
//		for(String unifiedSymbol : interestSymbols) {
//			List<String> availableDates = mdRepo.findDataAvailableDates(mktGatewayId, unifiedSymbol, true);
//			for(String date : availableDates.subList(availableDates.size() - info.getNumOfDaysOfDataRef(), availableDates.size())) {
//				List<MinBarDataPO> dataBarPOList = mdRepo.loadDataByDate(mktGatewayId, unifiedSymbol, date);
//				for(MinBarDataPO po : dataBarPOList) {
//					BarField.Builder bb = BarField.newBuilder();
//					ProtoBeanUtils.toProtoBean(bb, po);
//					barList.add(bb.build());
//				}
//			}
//			barDataList.add(new BarData(unifiedSymbol, barList));
//		}
//		
//		strategyModule.initMarketDataRef(barDataList);
//		strategyModule.setRunningStateChangeListener((isEnabled, module)->{
//			ModuleInfo moduleInfo = moduleRepo.findModuleInfo(module.getName());
//			moduleInfo.setEnabled(isEnabled);
//			moduleRepo.saveModuleInfo(moduleInfo);
//		});
//		
//		mdlMgr.addModule(strategyModule);
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
		return null;
	}
	
//	/**
//	 * 获取模组引用数据
//	 * @param moduleName
//	 * @return
//	 */
//	public ModuleDataRef getModuleDataRef(String moduleName) {
//		return mdlMgr.getModule(moduleName).getDataRef();
//	}
	
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
		moduleRepo.removeModulePosition(moduleName);
		moduleRepo.removeDealRecords(moduleName);
		moduleRepo.removeTradeRecords(moduleName);
	}
	
	
	/**
	 * 切换模组状态
	 */
	public boolean toggleState(String moduleName) {
		mdlMgr.getModule(moduleName).toggleRunningState();
		return true;
	}
	
	/**
	 * 新建模组持仓
	 * @param moduleName
	 * @param position
	 * @return
	 */
	public boolean createPosition(String moduleName, ModulePositionInfo position) {
		position.setOpenTime(System.currentTimeMillis());
		position.setOpenTradingDay(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER));
		return updatePosition(moduleName, position);
	}
	
	/**
	 * 更新模组持仓
	 * @param moduleName
	 * @param position
	 * @return
	 */
	public boolean updatePosition(String moduleName, ModulePositionInfo position) {
		ModulePosition mp = new ModulePosition(position, contractMgr.getContract(position.getUnifiedSymbol()));
		ModuleStatus moduleStatus = mdlMgr.getModule(moduleName).getModuleStatus();
		moduleStatus.addPosition(mp);
		List<ModulePositionInfo> posList = moduleStatus.getAllPositions().stream().map(ModulePosition::convertTo).collect(Collectors.toList());
		moduleRepo.saveModulePosition(ModulePositionPO.builder()
				.moduleName(moduleName)
				.positions(posList)
				.build());
		return true;
	}
	
	/**
	 * 移除模组持仓
	 * @param moduleName
	 * @param unifiedSymbol
	 * @param dir
	 * @return
	 */
	public boolean removePosition(String moduleName, String unifiedSymbol, PositionDirectionEnum dir) {
		ModuleStatus moduleStatus = mdlMgr.getModule(moduleName).getModuleStatus();
		moduleStatus.removePosition(unifiedSymbol, dir);
		List<ModulePositionInfo> posList = moduleStatus.getAllPositions().stream().map(ModulePosition::convertTo).collect(Collectors.toList());
		moduleRepo.saveModulePosition(ModulePositionPO.builder()
				.moduleName(moduleName)
				.positions(posList)
				.build());
		return true;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		for(ModuleInfo m : getCurrentModuleInfos()) {
			ModulePositionPO po = moduleRepo.loadModulePosition(m.getModuleName());
			loadModule(m, po.getPositions());
		}
	}
	
}
