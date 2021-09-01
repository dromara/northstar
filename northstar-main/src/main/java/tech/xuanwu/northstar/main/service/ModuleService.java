package tech.xuanwu.northstar.main.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.BeanUtils;
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
import tech.xuanwu.northstar.strategy.common.model.ModuleStatus;
import tech.xuanwu.northstar.strategy.common.model.StrategyModule;
import tech.xuanwu.northstar.strategy.common.model.data.BarData;
import tech.xuanwu.northstar.strategy.common.model.data.DealRecord;
import tech.xuanwu.northstar.strategy.common.model.data.ModuleCurrentPerformance;
import tech.xuanwu.northstar.strategy.common.model.meta.ComponentAndParamsPair;
import tech.xuanwu.northstar.strategy.common.model.meta.ComponentField;
import tech.xuanwu.northstar.strategy.common.model.meta.ComponentMetaInfo;
import tech.xuanwu.northstar.strategy.common.model.meta.DynamicParams;
import tech.xuanwu.northstar.strategy.common.model.persistence.ModuleStatusPO;
import tech.xuanwu.northstar.strategy.common.model.persistence.TradeDescriptionPO;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
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
		loadModule(info, new ModuleStatus(info.getModuleName(), contractMgr));
		return moduleRepo.saveModuleInfo(info);
	}
	
	/**
	 * 更新模组
	 * @param info
	 * @throws Exception 
	 */
	public boolean updateModule(ModuleInfo info) throws Exception {
		mdlMgr.removeModule(info.getModuleName());
		ModuleStatusPO mse = moduleRepo.loadModuleStatus(info.getModuleName());
		ModuleStatus status;
		if(mse == null) {
			status = new ModuleStatus(info.getModuleName(), contractMgr);
		} else {
			status = new ModuleStatus(mse, contractMgr);
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
	 * 获取模组当前绩效
	 * @param moduleName
	 * @return
	 */
	public ModuleCurrentPerformance getCurrentPerformance(String moduleName) {
		return mdlMgr.getModulePerformance(moduleName);
	}
	
	/**
	 * 获取模组成交历史
	 * @param moduleName
	 * @return
	 */
	public List<DealRecord> getHistoryRecords(String moduleName) {
		List<DealRecord> result = new ArrayList<>();
		Map<String, List<TradeDescriptionPO>> openingTradeMap = new HashMap<>();
		Map<String, List<TradeDescriptionPO>> closingTradeMap = new HashMap<>();
		List<TradeDescriptionPO> totalRecords = moduleRepo.findTradeDescription(moduleName);
		for(TradeDescriptionPO trade : totalRecords) {			
			String symbol = trade.getSymbol();
			if(trade.getOffsetFlag() == OffsetFlagEnum.OF_Open) {
				openingTradeMap.putIfAbsent(symbol, new LinkedList<>());
				openingTradeMap.get(symbol).add(trade);
			} else {
				closingTradeMap.putIfAbsent(symbol, new LinkedList<>());
				closingTradeMap.get(symbol).add(trade);
			}
		}
		
		for(Entry<String, List<TradeDescriptionPO>> e : closingTradeMap.entrySet()) {
			String curSymbol = e.getKey();
			LinkedList<TradeDescriptionPO> tempClosingTrade = new LinkedList<>();
			tempClosingTrade.addAll(e.getValue());
			
			LinkedList<TradeDescriptionPO> tempOpeningTrade = new LinkedList<>();
			tempOpeningTrade.addAll(openingTradeMap.get(curSymbol));
			
			while(tempClosingTrade.size() > 0) {
				TradeDescriptionPO closingDeal = tempClosingTrade.pollFirst();
				TradeDescriptionPO openingDeal = tempOpeningTrade.pollFirst();
				if(closingDeal == null || openingDeal == null 
						|| closingDeal.getTradeTimestamp() < openingDeal.getTradeTimestamp()) {
					throw new IllegalStateException("存在异常的平仓合约找不到对应的开仓合约");
				}
				PositionDirectionEnum dir = openingDeal.getDirection() == DirectionEnum.D_Buy ? PositionDirectionEnum.PD_Long 
						: openingDeal.getDirection() == DirectionEnum.D_Sell ? PositionDirectionEnum.PD_Short : PositionDirectionEnum.PD_Unknown;
				if(PositionDirectionEnum.PD_Unknown == dir) {
					throw new IllegalStateException("持仓方向不能确定");
				}
				int factor = PositionDirectionEnum.PD_Long == dir ? 1 : -1;
				double priceDiff = factor * (closingDeal.getPrice() - openingDeal.getPrice());
				int vol = Math.min(closingDeal.getVolume(), openingDeal.getVolume());
				int profit = (int) (priceDiff * closingDeal.getContractMultiplier() * vol);
				DealRecord deal = DealRecord.builder()
						.contractName(closingDeal.getSymbol())
						.direction(dir)
						.dealTimestamp(closingDeal.getTradeTimestamp())
						.openPrice(openingDeal.getPrice())
						.closePrice(closingDeal.getPrice())
						.tradingDay(closingDeal.getTradingDay())
						.volume(vol)
						.closeProfit(profit)
						.build();
				result.add(deal);
				int volDiff = Math.abs(closingDeal.getVolume() - openingDeal.getVolume());
				TradeDescriptionPO restTrade = new TradeDescriptionPO();
				BeanUtils.copyProperties(closingDeal, restTrade);
				restTrade.setVolume(volDiff);
				// 平仓手数多于开仓手数,则需要拆分平仓成交
				if(closingDeal.getVolume() > openingDeal.getVolume()) {
					tempClosingTrade.offerFirst(restTrade);
				}
				// 平仓手数少于开仓手数,则需要拆分开仓成交
				else if(closingDeal.getVolume() < openingDeal.getVolume()) {
					tempOpeningTrade.offerFirst(restTrade);
				}
			}
		}
		return result;
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
	public void toggleState(String moduleName) {
		mdlMgr.toggleState(moduleName);
		ModuleInfo info = moduleRepo.findModuleInfo(moduleName);
		info.setEnabled(!info.isEnabled());
		moduleRepo.saveModuleInfo(info);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		for(ModuleInfo m : getCurrentModuleInfos()) {
			ModuleStatusPO entity = moduleRepo.loadModuleStatus(m.getModuleName());
			ModuleStatus status;
			if(entity == null) {
				status = new ModuleStatus(m.getModuleName(), contractMgr);
			} else {
				status = new ModuleStatus(entity, contractMgr);
			}
			loadModule(m, status);
		}
	}
	
}
