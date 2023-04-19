package org.dromara.northstar.web.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.dromara.northstar.ExternalJarClassLoader;
import org.dromara.northstar.common.constant.Constants;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.constant.ModuleUsage;
import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.model.ComponentField;
import org.dromara.northstar.common.model.ComponentMetaInfo;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.MockTradeDescription;
import org.dromara.northstar.common.model.ModuleAccountDescription;
import org.dromara.northstar.common.model.ModuleAccountRuntimeDescription;
import org.dromara.northstar.common.model.ModuleDealRecord;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModulePositionDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.common.utils.MarketDataLoadingUtils;
import org.dromara.northstar.data.IModuleRepository;
import org.dromara.northstar.event.ModuleManager;
import org.dromara.northstar.gateway.api.IContractManager;
import org.dromara.northstar.gateway.api.utils.MarketDataRepoFactory;
import org.dromara.northstar.module.legacy.ModuleFactory;
import org.dromara.northstar.module.legacy.ModulePlaybackContext;
import org.dromara.northstar.strategy.api.DynamicParamsAware;
import org.dromara.northstar.strategy.api.IModule;
import org.dromara.northstar.strategy.api.annotation.StrategicComponent;
import org.dromara.northstar.web.PostLoadAware;
import org.springframework.context.ApplicationContext;

import com.alibaba.fastjson.JSONObject;

import cn.hutool.core.date.LocalDateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 
 * @author KevinHuangwl
 *
 */
@Slf4j
public class ModuleService implements PostLoadAware {
	
	private ApplicationContext ctx;
	
	private ModuleManager moduleMgr;
	
	private IContractManager contractMgr;
	
	private IModuleRepository moduleRepo;
	
	private MarketDataRepoFactory mdRepoFactory;
	
	private MarketDataLoadingUtils utils = new MarketDataLoadingUtils();
	
	private ModuleFactory moduleFactory;
	
	private ExternalJarClassLoader extJarLoader;
	
	public ModuleService(ApplicationContext ctx, ExternalJarClassLoader extJarLoader, IModuleRepository moduleRepo,
			MarketDataRepoFactory mdRepoFactory, ModuleFactory moduleFactory, ModuleManager moduleMgr, IContractManager contractMgr) {
		this.ctx = ctx;
		this.moduleMgr = moduleMgr;
		this.contractMgr = contractMgr;
		this.moduleRepo = moduleRepo;
		this.mdRepoFactory = mdRepoFactory;
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
		Map<String, ModuleAccountRuntimeDescription> accRtsMap = new HashMap<>();
		if(md.getUsage() == ModuleUsage.PLAYBACK) {
			accRtsMap.put(ModulePlaybackContext.PLAYBACK_GATEWAY, ModuleAccountRuntimeDescription.builder()
					.accountId(ModulePlaybackContext.PLAYBACK_GATEWAY)
					.initBalance(md.getModuleAccountSettingsDescription().get(0).getModuleAccountInitBalance())
					.preBalance(md.getModuleAccountSettingsDescription().get(0).getModuleAccountInitBalance())
					.positionDescription(new ModulePositionDescription())
					.build());
		} else {
			accRtsMap = md.getModuleAccountSettingsDescription().stream()
					.map(masd -> ModuleAccountRuntimeDescription.builder()
							.accountId(masd.getAccountGatewayId())
							.initBalance(masd.getModuleAccountInitBalance())
							.preBalance(masd.getModuleAccountInitBalance())
							.positionDescription(new ModulePositionDescription())
							.build())
					.collect(Collectors.toMap(ModuleAccountRuntimeDescription::getAccountId, mard -> mard));
		}
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
	public ModuleDescription modifyModule(ModuleDescription md, boolean reset) throws Exception {
		if(reset) {
			removeModule(md.getModuleName());
			return createModule(md);
		}
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
		int weeksOfDataForPreparation = md.getWeeksOfDataForPreparation();
		LocalDate date = LocalDate.now().minusWeeks(weeksOfDataForPreparation);
		
		IModule module = moduleFactory.newInstance(md, mrd);
		module.initModule();
		log.info("模组[{}] 初始化数据起始计算日为：{}", md.getModuleName(), date);
		LocalDateTime nowDateTime = LocalDateTime.now();
		LocalDate now = nowDateTime.getDayOfWeek().getValue() > 5 || nowDateTime.getDayOfWeek().getValue() == 5 && nowDateTime.toLocalTime().isAfter(LocalTime.of(20, 30))
				? LocalDate.now().plusWeeks(1)
				: LocalDate.now();
		// 模组数据初始化
		while(weeksOfDataForPreparation > 0
				&& toYearWeekVal(now) >= toYearWeekVal(date)) {
			LocalDate start = utils.getFridayOfThisWeek(date.minusWeeks(1));
			LocalDate end = utils.getFridayOfThisWeek(date);
			for(ModuleAccountDescription mad : md.getModuleAccountSettingsDescription()) {
				List<BarField> mergeList = new ArrayList<>();
				for(ContractSimpleInfo csi : mad.getBindedContracts()) {
					List<BarField> bars = mdRepoFactory.getInstance(mad.getAccountGatewayId()).loadBars(csi.getUnifiedSymbol(), start, end);
					mergeList.addAll(bars);
				}
				mergeList.sort((a,b) -> a.getActionTimestamp() < b.getActionTimestamp() ? -1 : 1);
				module.initData(mergeList);
			}
			date = date.plusWeeks(1);
		}
		module.setEnabled(mrd.isEnabled());
		moduleMgr.addModule(module);
	}
	
	// 把日期转换成年周，例如2022年第二周为202202
	private int toYearWeekVal(LocalDate date) {
		return date.getYear() * 100 + LocalDateTimeUtil.weekOfYear(date);
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
		ContractField contract = contractMgr.getContract(Identifier.of(mockTrade.getContractId())).contractField();
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
	public void postLoad() {
		log.info("开始加载模组");
		for(ModuleDescription md : findAllModules()) {
			try {				
				loadModule(md);
				Thread.sleep(10000); // 每十秒只能加载一个模组，避免数据服务被限流导致数据缺失
			} catch (Exception e) {
				log.warn("模组 [{}] 加载失败", md.getModuleName(), e);
			}
		}
		log.info("模组加载完毕");		
	}

}
