package org.dromara.northstar.web.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.dromara.northstar.account.AccountManager;
import org.dromara.northstar.common.IDataSource;
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
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.data.IMarketDataRepository;
import org.dromara.northstar.data.IModuleRepository;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.gateway.contract.OptionChainContract;
import org.dromara.northstar.gateway.mktdata.DataSourceDataLoader;
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
import org.dromara.northstar.support.utils.bar.BarMergerRegistry;
import org.dromara.northstar.web.PostLoadAware;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSONObject;

import cn.hutool.core.lang.Assert;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

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

	private AccountManager accountMgr;

	public ModuleService(ApplicationContext ctx, IModuleRepository moduleRepo, IMarketDataRepository mdRepo,
						 ModuleManager moduleMgr, IContractManager contractMgr, AccountManager accountMgr) {
		this.ctx = ctx;
		this.moduleMgr = moduleMgr;
		this.contractMgr = contractMgr;
		this.moduleRepo = moduleRepo;
		this.mdRepo = mdRepo;
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
	@Transactional
	@Override
	public ModuleDescription createModule(ModuleDescription md) throws Exception {
		Identifier moduleID = Identifier.of(md.getModuleName());
		Assert.isFalse(moduleMgr.contains(moduleID), String.format("模组名[%s]已经存在，无法新建模组", md.getModuleName()));
		log.info("增加模组 [{}]", md.getModuleName());
		ModuleAccountRuntimeDescription mard = ModuleAccountRuntimeDescription.builder()
				.initBalance(md.getInitBalance())
				.positionDescription(new ModulePositionDescription())
				.build();
		ModuleRuntimeDescription mrd = ModuleRuntimeDescription.builder()
				.moduleName(md.getModuleName())
				.enabled(false)
				.moduleState(ModuleState.EMPTY)
				.moduleAccountRuntime(mard)
				.storeObject(new JSONObject())
				.build();
		try {			
			loadModule(md, mrd);
			moduleRepo.saveRuntime(mrd);
			moduleRepo.saveSettings(md);
		} catch(Exception e) {
			moduleMgr.remove(moduleID);
			throw e;
		}
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
		strategy.setStoreObject(mrd.getStoreObject());
		IModuleContext moduleCtx = null;
		if(md.getUsage() == ModuleUsage.PLAYBACK) {
			mrd = ModuleRuntimeDescription.builder()
					.moduleName(md.getModuleName())
					.moduleState(ModuleState.EMPTY)
					.storeObject(new JSONObject())
					.moduleAccountRuntime(ModuleAccountRuntimeDescription.builder()
							.initBalance(md.getInitBalance())
							.build())
					.build();
			moduleCtx = new PlaybackModuleContext(strategy, md, mrd, contractMgr, moduleRepo, new BarMergerRegistry());
		} else {
			if(md.getType() == ModuleType.ARBITRAGE) {
				moduleCtx = new ArbitrageModuleContext(strategy, md, mrd, contractMgr, moduleRepo, new BarMergerRegistry());
			} else {
				moduleCtx = new ModuleContext(strategy, md, mrd, contractMgr, moduleRepo, new BarMergerRegistry());
			}
		}
		moduleMgr.add(new TradeModule(md, moduleCtx, accountMgr, contractMgr));
		strategy.setContext(moduleCtx);
		log.info("模组[{}] 初始化数据起始计算日为：{}", md.getModuleName(), date);
		
		final IModuleContext mctx = moduleCtx;
		if(md.getUsage() != ModuleUsage.PLAYBACK && weeksOfDataForPreparation > 0) {
			// 只有在非回测状态下，才需要预热数据
			for(ModuleAccountDescription mad : md.getModuleAccountSettingsDescription()) {
				for(ContractSimpleInfo csi : mad.getBindedContracts()) {
					IContract c = contractMgr.getContract(Identifier.of(csi.getValue()));
					IDataSource dataSrc = c.dataSource();
					Assert.notNull(dataSrc, "合约 [{}] 缺少数据源配置，无法加载历史数据", c.name());
					DataSourceDataLoader dataLoader = new DataSourceDataLoader(dataSrc);
					if(c instanceof OptionChainContract) {
						// 对于期权链合约，要加载的是成员合约
						c.memberContracts().forEach(rc -> loadDataForInit(rc, dataLoader, weeksOfDataForPreparation, mctx));
					} else {
						loadDataForInit(c, dataLoader, weeksOfDataForPreparation, mctx);
					}
				}
			}
		}
		
		moduleCtx.setEnabled(mrd.isEnabled());
		moduleCtx.onReady();
	}
	
	private void loadDataForInit(IContract c, DataSourceDataLoader loader, int weeks, IModuleContext mctx) {
		LocalDate from = LocalDate.now().minusWeeks(weeks);
		LocalDate to = LocalDate.now();
		loader.loadMinutelyData(c.contract(), from, to, bars -> mctx.initData(bars.reversed()));
		// 本地仅加载最近的数据
		List<Bar> data = mdRepo.loadBars(c, LocalDate.now(), LocalDate.now().plusDays(3));
		mctx.initData(data);
	}

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
		IContract c = contractMgr.getContract(Identifier.of(mockTrade.getContractId()));
		Contract contract = c.contract();
		IAccount account = module.getAccount(contract);
		String tradingDay = StringUtils.hasText(mockTrade.getTradeDate()) ? mockTrade.getTradeDate() : LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER);

		Trade trade = Trade.builder()
				.originOrderId(Constants.MOCK_ORDER_ID)
				.contract(contract)
				.tradeDate(LocalDate.now())
				.tradeTime(LocalTime.now())
				.tradeTimestamp(System.currentTimeMillis())
				.tradingDay(LocalDate.parse(tradingDay, DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.gatewayId(account.accountId())
				.direction(mockTrade.getDirection())
				.offsetFlag(mockTrade.getOffsetFlag())
				.price(mockTrade.getPrice())
				.volume(mockTrade.getVolume())
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
