package org.dromara.northstar.web.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.dromara.northstar.ExternalJarClassLoader;
import org.dromara.northstar.account.AccountManager;
import org.dromara.northstar.common.constant.Constants;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.constant.ModuleUsage;
import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.exception.NoSuchElementException;
import org.dromara.northstar.common.model.ComponentAndParamsPair;
import org.dromara.northstar.common.model.ComponentField;
import org.dromara.northstar.common.model.ComponentMetaInfo;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.GatewayDescription;
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
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.gateway.common.utils.MarketDataRepoFactory;
import org.dromara.northstar.module.ModuleContext;
import org.dromara.northstar.module.ModuleManager;
import org.dromara.northstar.module.PlaybackModuleContext;
import org.dromara.northstar.module.TradeModule;
import org.dromara.northstar.strategy.DynamicParamsAware;
import org.dromara.northstar.strategy.IAccount;
import org.dromara.northstar.strategy.IModule;
import org.dromara.northstar.strategy.IModuleContext;
import org.dromara.northstar.strategy.StrategicComponent;
import org.dromara.northstar.strategy.TradeStrategy;
import org.dromara.northstar.support.log.ModuleLoggerFactory;
import org.dromara.northstar.support.notification.MailDeliveryManager;
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
	
	private MailDeliveryManager mailMgr;
	
	private MarketDataLoadingUtils utils = new MarketDataLoadingUtils();
	
	private ModuleLoggerFactory moduleLoggerFactory = new ModuleLoggerFactory();
	
	private ExternalJarClassLoader extJarLoader;
	
	private AccountManager accountMgr;
	
	public ModuleService(ApplicationContext ctx, ExternalJarClassLoader extJarLoader, IModuleRepository moduleRepo, MailDeliveryManager mailMgr,
			MarketDataRepoFactory mdRepoFactory, ModuleManager moduleMgr, IContractManager contractMgr, AccountManager accountMgr) {
		this.ctx = ctx;
		this.moduleMgr = moduleMgr;
		this.contractMgr = contractMgr;
		this.moduleRepo = moduleRepo;
		this.mdRepoFactory = mdRepoFactory;
		this.extJarLoader = extJarLoader;
		this.mailMgr = mailMgr;
		this.accountMgr = accountMgr;
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
			accRtsMap.put(PlaybackModuleContext.PLAYBACK_GATEWAY, ModuleAccountRuntimeDescription.builder()
					.accountId(PlaybackModuleContext.PLAYBACK_GATEWAY)
					.initBalance(md.getModuleAccountSettingsDescription().get(0).getModuleAccountInitBalance())
					.positionDescription(new ModulePositionDescription())
					.build());
		} else {
			accRtsMap = md.getModuleAccountSettingsDescription().stream()
					.map(masd -> ModuleAccountRuntimeDescription.builder()
							.accountId(masd.getAccountGatewayId())
							.initBalance(masd.getModuleAccountInitBalance())
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
	@Transactional
	public ModuleDescription modifyModule(ModuleDescription md, boolean reset) throws Exception {
		if(reset) {
			removeModule(md.getModuleName());
			return createModule(md);
		}
		validateModify(md);
		unloadModule(md.getModuleName());
		loadModule(md);
		moduleRepo.saveSettings(md);
		return md;
	}
	
	private void validateModify(ModuleDescription md) {
		ModuleRuntimeDescription mrdOld = moduleRepo.findRuntimeByName(md.getModuleName());
		Map<String, ModuleAccountRuntimeDescription> mardMap = mrdOld.getAccountRuntimeDescriptionMap();
		boolean valid = true;
		for(ModuleAccountDescription mad : md.getModuleAccountSettingsDescription()) {
			if(!mardMap.containsKey(mad.getAccountGatewayId())) {
				valid = false;
				break;
			}
		}
		if(mardMap.size() != md.getModuleAccountSettingsDescription().size() || !valid) {
			throw new IllegalStateException("模组账户信息有重大变动，无法保存修改。如确实要修改，请使用【重置模组】");
		}
	}

	/**
	 * 删除模组
	 * @param name
	 * @return
	 */
	@Transactional
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
		
		ComponentAndParamsPair strategyComponent = md.getStrategySetting();
		TradeStrategy strategy = resolveComponent(strategyComponent);
		strategy.setStoreObject(mrd.getDataState());
		IModuleContext moduleCtx = null;
		if(md.getUsage() == ModuleUsage.PLAYBACK) {
			Map<String, ModuleAccountRuntimeDescription> mardMap = new HashMap<>();
			mardMap.put(PlaybackModuleContext.PLAYBACK_GATEWAY, ModuleAccountRuntimeDescription.builder()
					.accountId(PlaybackModuleContext.PLAYBACK_GATEWAY)
					.initBalance(md.getModuleAccountSettingsDescription().get(0).getModuleAccountInitBalance())
					.build());
			mrd = ModuleRuntimeDescription.builder()
					.moduleName(md.getModuleName())
					.moduleState(ModuleState.EMPTY)
					.dataState(new JSONObject())
					.accountRuntimeDescriptionMap(mardMap)
					.build();
			moduleCtx = new PlaybackModuleContext(strategy, md, mrd, contractMgr, moduleRepo, moduleLoggerFactory);
		} else {
			moduleCtx = new ModuleContext(strategy, md, mrd, contractMgr, moduleRepo, moduleLoggerFactory, mailMgr);
		}
		moduleMgr.add(new TradeModule(md, moduleCtx, accountMgr, contractMgr));
		strategy.setContext(moduleCtx);
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
				IAccount account = accountMgr.get(Identifier.of(mad.getAccountGatewayId()));
				GatewayDescription gd = account.getMarketGateway().gatewayDescription();
				List<BarField> mergeList = new ArrayList<>();
				for(ContractSimpleInfo csi : mad.getBindedContracts()) {
					List<BarField> bars = mdRepoFactory.getInstance(gd.getChannelType()).loadBars(csi.getUnifiedSymbol(), start, end);
					mergeList.addAll(bars);
				}
				mergeList.sort((a,b) -> a.getActionTimestamp() < b.getActionTimestamp() ? -1 : 1);
				moduleCtx.initData(mergeList);
			}
			date = date.plusWeeks(1);
		}
		moduleCtx.setEnabled(mrd.isEnabled());
	}
	
	@SuppressWarnings("unchecked")
	private <T extends DynamicParamsAware> T resolveComponent(ComponentAndParamsPair metaInfo) throws Exception {
		Map<String, ComponentField> fieldMap = new HashMap<>();
		for(ComponentField cf : metaInfo.getInitParams()) {
			fieldMap.put(cf.getName(), cf);
		}
		String clzName = metaInfo.getComponentMeta().getClassName();
		String paramClzName = clzName + "$InitParams";
		Class<?> type = null;
		Class<?> paramType = null;
		if(extJarLoader != null) {
			type = extJarLoader.loadClass(clzName);
			paramType = extJarLoader.loadClass(paramClzName);
		}
		if(type == null) {
			type = Class.forName(clzName);
			paramType = Class.forName(paramClzName);
		}
		
		DynamicParamsAware obj = (DynamicParamsAware) type.getDeclaredConstructor().newInstance();
		DynamicParams paramObj = (DynamicParams) paramType.getDeclaredConstructor().newInstance();
		paramObj.resolveFromSource(fieldMap);
		obj.initWithParams(paramObj);
		return (T) obj;
	}
	
	// 把日期转换成年周，例如2022年第二周为202202
	private int toYearWeekVal(LocalDate date) {
		return date.getYear() * 100 + LocalDateTimeUtil.weekOfYear(date);
	}
	
	
	private void unloadModule(String moduleName) {
		moduleMgr.remove(Identifier.of(moduleName));
		moduleRepo.deleteSettingsByName(moduleName);
	}
	
	/**
	 * 模组启停
	 * @param name
	 * @return
	 */
	public boolean toggleModule(String name) {
		IModule module = moduleMgr.get(Identifier.of(name));
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
		IModule module = moduleMgr.get(Identifier.of(name));
		if(Objects.isNull(module)) {
			throw new NoSuchElementException("没有找到模组：" + name);
		}
		return module.getRuntimeDescription();
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
		IModule module = moduleMgr.get(Identifier.of(moduleName));
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
