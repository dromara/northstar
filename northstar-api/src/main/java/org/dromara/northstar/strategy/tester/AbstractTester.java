package org.dromara.northstar.strategy.tester;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.IntStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.IGatewayService;
import org.dromara.northstar.common.IModuleService;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.ClosingPolicy;
import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.common.constant.ModuleType;
import org.dromara.northstar.common.constant.ModuleUsage;
import org.dromara.northstar.common.model.ComponentAndParamsPair;
import org.dromara.northstar.common.model.ComponentField;
import org.dromara.northstar.common.model.ComponentMetaInfo;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.ModuleAccountDescription;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.gateway.Contract;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.gateway.MarketGateway;
import org.dromara.northstar.gateway.TradeGateway;
import org.dromara.northstar.strategy.IModule;
import org.springframework.beans.BeanUtils;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreField.ContractField;

@Slf4j
public abstract class AbstractTester {
	
	protected IContractManager contractMgr;
	
	protected GatewayBuilder gatewayBuilder;
	
	protected ModuleBuilder moduleBuilder;
	
	protected IGatewayService gatewayService;
	
	protected IModuleService moduleService;
	
	protected ModuleTesterContext ctx;
	
	protected AbstractTester(GatewayBuilder gatewayBuilder, ModuleBuilder moduleBuilder, IContractManager contractMgr,
			IGatewayService gatewayService, IModuleService moduleService) {
		this.contractMgr = contractMgr;
		this.gatewayBuilder = gatewayBuilder;
		this.moduleBuilder = moduleBuilder;
		this.gatewayService = gatewayService;
		this.moduleService = moduleService;
	}
	
	// 策略配置
	public abstract DynamicParams strategyParams(ContractSimpleInfo csi);
	
	// 策略信息
	public abstract ComponentMetaInfo strategy();
	
	// 测试周期
	public abstract int[] testPeriods();
	
	private List<ComponentField> convertParams(DynamicParams params){
		Map<String, ComponentField> fieldMap = params.getMetaInfo();
		for(Entry<String, ComponentField> e : fieldMap.entrySet()) {
			PropertyDescriptor prop = BeanUtils.getPropertyDescriptor(params.getClass(), e.getKey());
			try {
				Object value = prop.getReadMethod().invoke(params);
				e.getValue().setValue(value);
			} catch (Exception ex) {
				log.error("", ex);
			}
		}
		return fieldMap.values().stream().toList();
	}
	
	// 开始测试 
	public void start() {
		String currentSymbol = load();
		for(String symbol : ctx.testSymbols()) {
			if(StringUtils.isNotEmpty(currentSymbol) && !StringUtils.equals(symbol, currentSymbol)) {
				continue;
			}
			currentSymbol = "";
			save(symbol);
			
			///////////////////////////////////////
			/// 			准备工作 				///
			///////////////////////////////////////
			String idxSymbol = symbol + "0000";
			Contract c = contractMgr.getContract(ChannelType.PLAYBACK, idxSymbol);
			ContractField cf = c.contractField();
			ContractSimpleInfo csi = ContractSimpleInfo.builder()
					.name(cf.getName())
					.channelType(ChannelType.PLAYBACK)
					.unifiedSymbol(cf.getUnifiedSymbol())
					.value(c.identifier().value())
					.build();
			MarketGateway mktGateway = gatewayBuilder.createPlaybackGateway(csi, ctx);
			TradeGateway tdGateway = gatewayBuilder.createSimGateway(mktGateway);
			tdGateway.connect();
			gatewayService.simMoneyIO(tdGateway.gatewayId(), ctx.symbolTestAmount().get(symbol));
			ModuleAccountDescription mad = ModuleAccountDescription.builder()
					.accountGatewayId(tdGateway.gatewayId())
					.bindedContracts(List.of(csi))
					.build();
			ComponentAndParamsPair strategySettings = ComponentAndParamsPair.builder()
					.componentMeta(strategy())
					.initParams(convertParams(strategyParams(csi)))
					.build();
			List<IModule> testModules = IntStream.of(testPeriods())
					.mapToObj(min -> moduleBuilder.createModule(ModuleDescription.builder()
										.moduleName(String.format("%s%d分钟", symbol, min))
										.initBalance(ctx.symbolTestAmount().get(symbol))
										.usage(ModuleUsage.PLAYBACK)
										.type(ModuleType.SPECULATION)
										.closingPolicy(ClosingPolicy.FIRST_IN_FIRST_OUT)
										.numOfMinPerBar(min)
										.moduleCacheDataSize(5000)
										.strategySetting(strategySettings)
										.moduleAccountSettingsDescription(List.of(mad))
										.build()))
					.toList();

			///////////////////////////////////////
			/// 			数据预热 				///
			///////////////////////////////////////
			mktGateway.connect();
			while (mktGateway.getConnectionState() != ConnectionState.DISCONNECTED) {
				pause(5);
				log.info("{} 数据预热中", idxSymbol);
			}

			///////////////////////////////////////
			/// 			开始回测 				///
			///////////////////////////////////////
			testModules.forEach(m -> m.setEnabled(true));
			pause(1);
			mktGateway.connect();
			while (mktGateway.getConnectionState() != ConnectionState.DISCONNECTED) {
				pause(60);
				log.info("{} 回测中", idxSymbol);
			}
			pause(30); // 等待计算结束

			///////////////////////////////////////
			/// 			统计结果 				///
			///////////////////////////////////////
			for(IModule m : testModules) {
				ModuleRuntimeDescription mrd = m.getModuleContext().getRuntimeDescription(false);
				double totalEarning = mrd.getAccountRuntimeDescription().getAccCloseProfit() - mrd.getAccountRuntimeDescription().getAccCommission();
				double earningRiskRate = totalEarning / Math.abs(mrd.getAccountRuntimeDescription().getMaxDrawback());
				if (mrd.getAccountRuntimeDescription().getAccCloseProfit() < 0 
						|| mrd.getAccountRuntimeDescription().getMaxDrawbackPercentage() > 0.5
						|| earningRiskRate < 2 ) { 
					moduleService.removeModule(m.getName());
				}
			}
		}
		save("");
	}
	
	private void save(String symbol) {
		File saveFile = new File("data/module-tester/" + strategy().getName());
		try {
			FileUtils.write(saveFile, symbol, StandardCharsets.UTF_8);
		} catch (IOException e) {
			log.error("", e);
		}
	}
	
	private String load() {
		File saveFile = new File("data/module-tester/" + strategy().getName());
		try {
			if(saveFile.exists()) {
				return FileUtils.readFileToString(saveFile, StandardCharsets.UTF_8);
			}
		} catch (IOException e) {
			log.error("", e);
		}
		return "";
	}
	
	private void pause(int sec) {
		try {
			Thread.sleep(sec * 1000L);
		} catch (InterruptedException e) {
			log.warn("", e);
		}
	}
}
