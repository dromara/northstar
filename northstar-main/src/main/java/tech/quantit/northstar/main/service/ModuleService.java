package tech.quantit.northstar.main.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;

import com.alibaba.fastjson.JSONObject;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.Constants;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.common.constant.GatewayType;
import tech.quantit.northstar.common.constant.ModuleState;
import tech.quantit.northstar.common.event.NorthstarEvent;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.model.ComponentField;
import tech.quantit.northstar.common.model.ComponentMetaInfo;
import tech.quantit.northstar.common.model.DynamicParams;
import tech.quantit.northstar.common.model.MockTradeDescription;
import tech.quantit.northstar.common.model.ModuleAccountDescription;
import tech.quantit.northstar.common.model.ModuleAccountRuntimeDescription;
import tech.quantit.northstar.common.model.ModuleDealRecord;
import tech.quantit.northstar.common.model.ModuleDescription;
import tech.quantit.northstar.common.model.ModulePositionDescription;
import tech.quantit.northstar.common.model.ModuleRuntimeDescription;
import tech.quantit.northstar.common.utils.ContractUtils;
import tech.quantit.northstar.common.utils.MarketDataLoadingUtils;
import tech.quantit.northstar.data.IMarketDataRepository;
import tech.quantit.northstar.data.IModuleRepository;
import tech.quantit.northstar.domain.gateway.ContractManager;
import tech.quantit.northstar.main.ExternalJarClassLoader;
import tech.quantit.northstar.main.handler.internal.ModuleManager;
import tech.quantit.northstar.main.utils.ModuleFactory;
import tech.quantit.northstar.strategy.api.DynamicParamsAware;
import tech.quantit.northstar.strategy.api.IModule;
import tech.quantit.northstar.strategy.api.annotation.StrategicComponent;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 
 * @author KevinHuangwl
 *
 */
@Slf4j
public class ModuleService implements InitializingBean {
	
	private ApplicationContext ctx;
	
	private ModuleManager moduleMgr;
	
	private ContractManager contractMgr;
	
	private IModuleRepository moduleRepo;
	
	private IMarketDataRepository mdRepo;
	
	private MarketDataLoadingUtils utils = new MarketDataLoadingUtils();
	
	private ModuleFactory moduleFactory;
	
	private ExternalJarClassLoader extJarLoader;
	
	public ModuleService(ApplicationContext ctx, ExternalJarClassLoader extJarLoader, IModuleRepository moduleRepo, IMarketDataRepository mdRepo,
			ModuleFactory moduleFactory, ModuleManager moduleMgr, ContractManager contractMgr) {
		this.ctx = ctx;
		this.moduleMgr = moduleMgr;
		this.contractMgr = contractMgr;
		this.moduleRepo = moduleRepo;
		this.mdRepo = mdRepo;
		this.moduleFactory = moduleFactory;
		this.extJarLoader = extJarLoader;
	}

	/**
	 * 获取全部交易策略
	 * @return
	 */
	public List<ComponentMetaInfo> getRegisteredTradeStrategies(){
		return getComponentMeta();
	}
	
	private List<ComponentMetaInfo> getComponentMeta() {
		Map<String, Object> objMap = ctx.getBeansWithAnnotation(StrategicComponent.class);
		List<ComponentMetaInfo> result = new ArrayList<>(objMap.size());
		for (Entry<String, Object> e : objMap.entrySet()) {
			StrategicComponent anno = e.getValue().getClass().getAnnotation(StrategicComponent.class);
			result.add(new ComponentMetaInfo(anno.value(), e.getValue().getClass().getName()));
		}
		return result;
	}
	
	/**
	 * 获取策略配置元信息
	 * @param metaInfo
	 * @return
	 * @throws ClassNotFoundException
	 */
	public Map<String, ComponentField> getComponentParams(ComponentMetaInfo metaInfo) throws ClassNotFoundException {
		String className = metaInfo.getClassName();
		Class<?> clz = null;
		if(extJarLoader != null) {
			clz = extJarLoader.loadClass(className);
		}
		if(clz == null) {			
			clz = Class.forName(className);
		}
		DynamicParamsAware aware = (DynamicParamsAware) ctx.getBean(clz);
		DynamicParams params = aware.getDynamicParams();
		return params.getMetaInfo();
	}
	
	/**
	 * 增加模组
	 * @param md
	 * @return
	 * @throws Exception 
	 */
	public ModuleDescription createModule(ModuleDescription md) throws Exception {
		Map<String, ModuleAccountRuntimeDescription> accRtsMap = md.getModuleAccountSettingsDescription().stream()
				.map(masd -> ModuleAccountRuntimeDescription.builder()
						.accountId(masd.getAccountGatewayId())
						.initBalance(masd.getModuleAccountInitBalance())
						.preBalance(masd.getModuleAccountInitBalance())
						.positionDescription(new ModulePositionDescription())
						.build())
				.collect(Collectors.toMap(ModuleAccountRuntimeDescription::getAccountId, mard -> mard));
		ModuleRuntimeDescription mad = ModuleRuntimeDescription.builder()
				.moduleName(md.getModuleName())
				.enabled(false)
				.moduleState(ModuleState.EMPTY)
				.accountRuntimeDescriptionMap(accRtsMap)
				.dataState(new JSONObject())
				.build();
		moduleRepo.saveRuntime(mad);
		moduleRepo.saveSettings(md);
		loadModule(md);
		return md;
	}
	
	/**
	 * 修改模组
	 * @param md
	 * @return
	 * @throws Exception 
	 */
	public ModuleDescription modifyModule(ModuleDescription md) throws Exception {
		unloadModule(md.getModuleName());
		loadModule(md);
		moduleRepo.saveSettings(md);
		return md;
	}
	
	/**
	 * 删除模组
	 * @param name
	 * @return
	 */
	public boolean removeModule(String name) {
		unloadModule(name);
		moduleRepo.deleteRuntimeByName(name);
		moduleRepo.removeAllDealRecords(name);
		return true;
	}
	
	/**
	 * 查询模组
	 * @return
	 */
	public List<ModuleDescription> findAllModules() {
		return moduleRepo.findAllSettings();
	}
	
	private void loadModule(ModuleDescription md) throws Exception {
		ModuleRuntimeDescription mrd = moduleRepo.findRuntimeByName(md.getModuleName());
		int daysOfDataForPreparation = md.getDaysOfDataForPreparation();
		int year = LocalDate.now().getYear();
		List<LocalDate> datesLastYear = mdRepo.findHodidayInLaw(GatewayType.CTP, year - 1);
		List<LocalDate> datesThisYear = mdRepo.findHodidayInLaw(GatewayType.CTP, year);
		Set<LocalDate> holidays = new HashSet<>();
		holidays.addAll(datesLastYear);
		holidays.addAll(datesThisYear);
		LocalDate date = LocalDate.now();
		while(daysOfDataForPreparation > 0) {
			if(!holidays.contains(date)) {
				daysOfDataForPreparation--;
			}
			date = date.minusDays(1);
		}
		
		IModule module = moduleFactory.newInstance(md, mrd);
		module.initModule();
		log.info("模组[{}] 初始化数据起始计算日为：{}", md.getModuleName(), date);
		// 模组数据初始化
		while(LocalDate.now().isAfter(date)) {
			LocalDate start = utils.getFridayOfThisWeek(date.minusWeeks(1));
			LocalDate end = utils.getFridayOfThisWeek(date);
			for(ModuleAccountDescription mad : md.getModuleAccountSettingsDescription()) {
				for(String unifiedSymbol : mad.getBindedUnifiedSymbols()) {
					ContractField contract = contractMgr.getContract(unifiedSymbol);
					List<BarField> bars = mdRepo.loadBars(ContractUtils.getMarketGatewayId(contract), unifiedSymbol, start, end);
					module.initData(bars);
				}
			}
			date = date.plusWeeks(1);
		}
		module.setEnabled(mrd.isEnabled());
		moduleMgr.addModule(module);
	}
	
	private void unloadModule(String moduleName) {
		moduleMgr.removeModule(moduleName);
		moduleRepo.deleteSettingsByName(moduleName);
	}
	
	/**
	 * 模组启停
	 * @param name
	 * @return
	 */
	public boolean toggleModule(String name) {
		IModule module = moduleMgr.getModule(name);
		boolean flag = !module.isEnabled();
		module.setEnabled(flag);
		return flag;
	}
	
	/**
	 * 模组运行时状态
	 * @param name
	 * @return
	 */
	public ModuleRuntimeDescription getModuleRealTimeInfo(String name) {
		return moduleMgr.getModule(name).getRuntimeDescription();
	}
	
	/**
	 * 模组交易历史
	 * @param name
	 * @return
	 */
	public List<ModuleDealRecord> getDealRecords(String name){
		return moduleRepo.findAllDealRecords(name);
	}
	
	/**
	 * 持仓调整
	 * @return
	 */
	public boolean mockTradeAdjustment(String moduleName, MockTradeDescription mockTrade) {
		IModule module = moduleMgr.getModule(moduleName);
		ContractField contract = contractMgr.getContract(mockTrade.getUnifiedSymbol());
		TradeField trade = TradeField.newBuilder()
				.setOriginOrderId(Constants.MOCK_ORDER_ID)
				.setContract(contract)
				.setTradeDate(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER) + "MT")
				.setTradingDay(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER) + "MT")
				.setGatewayId(mockTrade.getGatewayId())
				.setDirection(mockTrade.getDirection())
				.setOffsetFlag(mockTrade.getOffsetFlag())
				.setPrice(mockTrade.getPrice())
				.setVolume(mockTrade.getVolume())
				.build();
		module.onEvent(new NorthstarEvent(NorthstarEventType.TRADE, trade));
		return true;
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		for(ModuleDescription md : findAllModules()) {
			loadModule(md);
		}
	}
	
}
