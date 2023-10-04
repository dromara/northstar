package org.dromara.northstar.web.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.dromara.northstar.account.AccountManager;
import org.dromara.northstar.common.IModuleService;
import org.dromara.northstar.common.constant.Constants;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.constant.ModuleType;
import org.dromara.northstar.common.constant.ModuleUsage;
import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.model.ComponentAndParamsPair;
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
import org.dromara.northstar.data.IMarketDataRepository;
import org.dromara.northstar.data.IModuleRepository;
import org.dromara.northstar.gateway.Contract;
import org.dromara.northstar.gateway.GatewayMetaProvider;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.module.ArbitrageModuleContext;
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
import org.dromara.northstar.support.utils.bar.BarMergerRegistry;
import org.dromara.northstar.web.PostLoadAware;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSONObject;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.lang.Assert;
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
public class ModuleService implements IModuleService, PostLoadAware {
	
	private ApplicationContext ctx;
	
	private ModuleManager moduleMgr;
	
	private IContractManager contractMgr;
	
	private IModuleRepository moduleRepo;
	
	private IMarketDataRepository mdRepo;
	
	private MarketDataLoadingUtils utils = new MarketDataLoadingUtils();
	
	private ModuleLoggerFactory moduleLoggerFactory = new ModuleLoggerFactory();
	
	private AccountManager accountMgr;
	
	private GatewayMetaProvider gatewayMetaProvider;
	
	public ModuleService(ApplicationContext ctx, IModuleRepository moduleRepo, IMarketDataRepository mdRepo, 
			ModuleManager moduleMgr, IContractManager contractMgr, AccountManager accountMgr, GatewayMetaProvider gatewayMetaProvider) {
		this.ctx = ctx;
		this.moduleMgr = moduleMgr;
		this.contractMgr = contractMgr;
		this.moduleRepo = moduleRepo;
		this.mdRepo = mdRepo;
		this.accountMgr = accountMgr;
		this.gatewayMetaProvider = gatewayMetaProvider;
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
		Class<?> clz = Class.forName(className);
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
	@Override
	public ModuleDescription createModule(ModuleDescription md) throws Exception {
		log.info("增加模组 [{}]", md.getModuleName());
		ModuleAccountRuntimeDescription mard = ModuleAccountRuntimeDescription.builder()
				.initBalance(md.getInitBalance())
				.positionDescription(new ModulePositionDescription())
				.build();
		ModuleRuntimeDescription mrd = ModuleRuntimeDescription.builder()
				.moduleName(md.getModuleName())
				.enabled(false)
				.moduleState(ModuleState.EMPTY)
				.accountRuntimeDescription(mard)
				.dataState(new JSONObject())
				.build();
		loadModule(md, mrd);
		moduleRepo.saveRuntime(mrd);
		moduleRepo.saveSettings(md);
		return md;
	}
	
	/**
	 * 修改模组
	 * @param md
	 * @return
	 * @throws Exception 
	 */
	@Transactional
	@Override
	public ModuleDescription modifyModule(ModuleDescription md, boolean reset) throws Exception {
		if(reset) {
			log.info("重置模组 [{}]", md.getModuleName());
			removeModule(md.getModuleName());
			return createModule(md);
		}
		log.info("更新模组 [{}]", md.getModuleName());
		validateChange(md);
		unloadModule(md.getModuleName());
		loadModule(md, moduleRepo.findRuntimeByName(md.getModuleName()));
		moduleRepo.saveSettings(md);
		return md;
	}
	
	// 更新合法性校验：持仓状态下，模组不允许修改绑定的账户与合约信息
	private void validateChange(ModuleDescription md) {
		IModule module = moduleMgr.get(Identifier.of(md.getModuleName()));
		ModuleDescription md0 = module.getModuleDescription();
		if(!module.getModuleContext().getState().isEmpty()) {
			Set<String> accountNames = md.getModuleAccountSettingsDescription().stream().map(ModuleAccountDescription::getAccountGatewayId).collect(Collectors.toSet());
			Set<String> accountNames0 = md0.getModuleAccountSettingsDescription().stream().map(ModuleAccountDescription::getAccountGatewayId).collect(Collectors.toSet());
			Assert.isTrue(accountNames.equals(accountNames0), "模组在持仓状态下，不能修改绑定账户");
			
			Set<String> bindedContracts = md.getModuleAccountSettingsDescription().stream().flatMap(mad -> mad.getBindedContracts().stream()).map(ContractSimpleInfo::getUnifiedSymbol).collect(Collectors.toSet());
			Set<String> bindedContracts0 = md0.getModuleAccountSettingsDescription().stream().flatMap(mad -> mad.getBindedContracts().stream()).map(ContractSimpleInfo::getUnifiedSymbol).collect(Collectors.toSet());
			Assert.isTrue(bindedContracts.equals(bindedContracts0), "模组在持仓状态下，不能修改绑定合约");
		}
	}
	
	/**
	 * 删除模组
	 * @param name
	 * @return
	 */
	@Transactional
	@Override
	public boolean removeModule(String name) {
		log.info("删除模组 [{}]", name);
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
	
	private void loadModule(ModuleDescription md, ModuleRuntimeDescription mrd) throws Exception {
		int weeksOfDataForPreparation = md.getWeeksOfDataForPreparation();
		LocalDate date = LocalDate.now().minusWeeks(weeksOfDataForPreparation);
		
		ComponentAndParamsPair strategyComponent = md.getStrategySetting();
		TradeStrategy strategy = resolveComponent(strategyComponent);
		Assert.isTrue(strategy.type() == md.getType(), "该策略只能用于类型为[{}]的模组", strategy.type());
		strategy.setStoreObject(mrd.getDataState());
		IModuleContext moduleCtx = null;
		if(md.getUsage() == ModuleUsage.PLAYBACK) {
			mrd = ModuleRuntimeDescription.builder()
					.moduleName(md.getModuleName())
					.moduleState(ModuleState.EMPTY)
					.dataState(new JSONObject())
					.accountRuntimeDescription(ModuleAccountRuntimeDescription.builder()
							.initBalance(md.getInitBalance())
							.build())
					.build();
			moduleCtx = new PlaybackModuleContext(strategy, md, mrd, contractMgr, moduleRepo, moduleLoggerFactory, new BarMergerRegistry(gatewayMetaProvider));
		} else {
			if(md.getType() == ModuleType.ARBITRAGE) {
				moduleCtx = new ArbitrageModuleContext(strategy, md, mrd, contractMgr, moduleRepo, moduleLoggerFactory, new BarMergerRegistry(gatewayMetaProvider));
			} else {				
				moduleCtx = new ModuleContext(strategy, md, mrd, contractMgr, moduleRepo, moduleLoggerFactory, new BarMergerRegistry(gatewayMetaProvider));
			}
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
			List<BarField> mergeList = new ArrayList<>();
			for(ModuleAccountDescription mad : md.getModuleAccountSettingsDescription()) {
				for(ContractSimpleInfo csi : mad.getBindedContracts()) {
					Contract c = contractMgr.getContract(Identifier.of(csi.getValue()));
					List<BarField> bars = mdRepo.loadBars(c.contractField(), start, end);
					mergeList.addAll(bars);
				}
			}
			moduleCtx.initData(mergeList.parallelStream().sorted(sortFunction).toList());
			date = date.plusWeeks(1);
		}
		moduleCtx.setEnabled(mrd.isEnabled());
		moduleCtx.onReady();
	}
	
	private Comparator<BarField> sortFunction = (a, b) -> a.getActionTimestamp() < b.getActionTimestamp() ? -1 
													: a.getActionTimestamp() > b.getActionTimestamp() ? 1 : 0;
	
	@SuppressWarnings("unchecked")
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
		log.info("切换模组启停状态：[{}] {} -> {}", name, module.isEnabled(), flag);
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
			log.warn("没有找到模组：{}", name);
			return null;
		}
		return module.getRuntimeDescription();
	}
	
	/**
	 * 模组持仓状态
	 * @param name
	 * @return
	 */
	public ModuleState getModuleState(String name) {
		IModule module = moduleMgr.get(Identifier.of(name));
		if(Objects.isNull(module)) {
			log.warn("没有找到模组：{}", name);
			return null;
		}
		return module.getModuleContext().getState();
	}
	
	/**
	 * 模组启停状态
	 * @param name
	 * @return
	 */
	public Boolean hasModuleEnabled(String name) {
		IModule module = moduleMgr.get(Identifier.of(name));
		if(Objects.isNull(module)) {
			log.warn("没有找到模组：{}", name);
			return null;
		}
		if(!module.getModuleContext().isReady()) {
			log.info("模组 [{}] 仍在加载中", name);
			return null;
		}
		return module.isEnabled();
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
		Contract c = contractMgr.getContract(Identifier.of(mockTrade.getContractId()));
		ContractField contract = c.contractField();
		IAccount account = module.getAccount(c);
		String tradingDay = StringUtils.hasText(mockTrade.getTradeDate()) ? mockTrade.getTradeDate() : LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER);
		
		TradeField trade = TradeField.newBuilder()
				.setOriginOrderId(Constants.MOCK_ORDER_ID)
				.setContract(contract)
				.setTradeDate(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.setTradeTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) + "MT")
				.setTradeTimestamp(System.currentTimeMillis())
				.setTradingDay(tradingDay)
				.setGatewayId(account.accountId())
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
				ModuleRuntimeDescription mrd = moduleRepo.findRuntimeByName(md.getModuleName());
				loadModule(md, mrd);
			} catch (Exception e) {
				log.warn(String.format("模组 [%s] 加载失败。原因：", md.getModuleName()), e);
			}
		}
		log.info("模组加载完毕");		
	}

}
