package tech.quantit.northstar.main.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationContext;

import com.alibaba.fastjson.JSONObject;

import cn.hutool.core.date.LocalDateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.ChannelType;
import tech.quantit.northstar.common.constant.Constants;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.common.constant.ModuleState;
import tech.quantit.northstar.common.constant.ModuleUsage;
import tech.quantit.northstar.common.event.NorthstarEvent;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.model.ComponentField;
import tech.quantit.northstar.common.model.ComponentMetaInfo;
import tech.quantit.northstar.common.model.ContractSimpleInfo;
import tech.quantit.northstar.common.model.DynamicParams;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.common.model.Identifier;
import tech.quantit.northstar.common.model.MockTradeDescription;
import tech.quantit.northstar.common.model.ModuleAccountDescription;
import tech.quantit.northstar.common.model.ModuleAccountRuntimeDescription;
import tech.quantit.northstar.common.model.ModuleDealRecord;
import tech.quantit.northstar.common.model.ModuleDescription;
import tech.quantit.northstar.common.model.ModulePositionDescription;
import tech.quantit.northstar.common.model.ModuleRuntimeDescription;
import tech.quantit.northstar.common.utils.MarketDataLoadingUtils;
import tech.quantit.northstar.data.IGatewayRepository;
import tech.quantit.northstar.data.IMarketDataRepository;
import tech.quantit.northstar.data.IModuleRepository;
import tech.quantit.northstar.domain.module.ModulePlaybackContext;
import tech.quantit.northstar.gateway.api.IContractManager;
import tech.quantit.northstar.main.ExternalJarClassLoader;
import tech.quantit.northstar.main.PostLoadAware;
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
public class ModuleService implements PostLoadAware {
	
	private ApplicationContext ctx;
	
	private ModuleManager moduleMgr;
	
	private IContractManager contractMgr;
	
	private IModuleRepository moduleRepo;
	
	private IMarketDataRepository mdRepo;
	
	private MarketDataLoadingUtils utils = new MarketDataLoadingUtils();
	
	private ModuleFactory moduleFactory;
	
	private ExternalJarClassLoader extJarLoader;
	
	private IGatewayRepository gatewayRepo;
	
	public ModuleService(ApplicationContext ctx, ExternalJarClassLoader extJarLoader, IModuleRepository moduleRepo, IMarketDataRepository mdRepo, 
			IGatewayRepository gatewayRepo, ModuleFactory moduleFactory, ModuleManager moduleMgr, IContractManager contractMgr) {
		this.ctx = ctx;
		this.moduleMgr = moduleMgr;
		this.contractMgr = contractMgr;
		this.gatewayRepo = gatewayRepo;
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
		checkAccountWithCorrectUsage(md);
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
	
	private void checkAccountWithCorrectUsage(ModuleDescription md) {
		for(ModuleAccountDescription mad : md.getModuleAccountSettingsDescription()) {
			GatewayDescription accgd = gatewayRepo.findById(mad.getAccountGatewayId());
			GatewayDescription mktgd = gatewayRepo.findById(accgd.getBindedMktGatewayId());
			switch(md.getUsage()) {
			case PLAYBACK -> {
				if(mktgd.getChannelType() != ChannelType.PLAYBACK) {
					throw new IllegalArgumentException(String.format("账户 [%s] 不能用于回测", mktgd.getGatewayId()));
				}
			}
			case PROD -> {
				if(mktgd.getChannelType() == ChannelType.SIM) {
					throw new IllegalArgumentException("模拟行情不能用于实盘");
				}
				if(accgd.getChannelType() == ChannelType.SIM) {
					throw new IllegalArgumentException("模拟账户不能用于实盘");
				}
				if(mktgd.getChannelType() == ChannelType.PLAYBACK) {
					throw new IllegalArgumentException("历史行情不能用于实盘");
				}
			}
			case UAT -> {
				if(accgd.getChannelType() != ChannelType.SIM) {
					throw new IllegalArgumentException("模拟盘只能使用模拟账户");
				}
			}
			default -> throw new IllegalStateException("未知用途:" + md.getUsage());
			}
		}
	}

	/**
	 * 修改模组
	 * @param md
	 * @return
	 * @throws Exception 
	 */
	public ModuleDescription modifyModule(ModuleDescription md, boolean reset) throws Exception {
		checkAccountWithCorrectUsage(md);
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
		autoUpdate(md, mrd);
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
					List<BarField> bars = mdRepo.loadBars(csi.getUnifiedSymbol(), start, end);
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
	
	private void autoUpdate(ModuleDescription md, ModuleRuntimeDescription mrd) {
		Map<String, ModuleAccountRuntimeDescription> oldMardMap = mrd.getAccountRuntimeDescriptionMap();
		Map<String, ModuleAccountRuntimeDescription> newMardMap = new HashMap<>();
		for(ModuleAccountDescription mad : md.getModuleAccountSettingsDescription()) {
			if(oldMardMap.containsKey(mad.getAccountGatewayId())) {
				ModuleAccountRuntimeDescription oldMard = oldMardMap.get(mad.getAccountGatewayId());
				oldMard.setInitBalance(mad.getModuleAccountInitBalance());
				newMardMap.put(mad.getAccountGatewayId(), oldMard);
			} else {
				newMardMap.put(mad.getAccountGatewayId(), ModuleAccountRuntimeDescription.builder()
						.accountId(mad.getAccountGatewayId())
						.initBalance(mad.getModuleAccountInitBalance())
						.preBalance(mad.getModuleAccountInitBalance())
						.positionDescription(new ModulePositionDescription())
						.build());
			}
		}
		mrd.setAccountRuntimeDescriptionMap(newMardMap);
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
