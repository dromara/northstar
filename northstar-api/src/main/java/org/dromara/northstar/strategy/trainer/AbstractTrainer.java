package org.dromara.northstar.strategy.trainer;

import java.util.List;
import java.util.stream.IntStream;

import org.dromara.northstar.common.IGatewayService;
import org.dromara.northstar.common.IModuleService;
import org.dromara.northstar.common.ObjectManager;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.ClosingPolicy;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.constant.GatewayUsage;
import org.dromara.northstar.common.constant.ModuleType;
import org.dromara.northstar.common.constant.ModuleUsage;
import org.dromara.northstar.common.model.ComponentAndParamsPair;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.ModuleAccountDescription;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.gateway.Contract;
import org.dromara.northstar.gateway.Gateway;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.gateway.MarketGateway;
import org.dromara.northstar.gateway.TradeGateway;
import org.dromara.northstar.strategy.IModule;
import org.dromara.northstar.strategy.tester.AbstractTester;
import org.dromara.northstar.strategy.tester.ModuleTesterContext;

import com.alibaba.fastjson2.JSONObject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractTrainer extends AbstractTester implements RLAgentTrainingContext{

	protected AbstractTrainer(ObjectManager<Gateway> gatewayMgr, ObjectManager<IModule> moduleMgr, IContractManager contractMgr,
			IGatewayService gatewayService, IModuleService moduleService) {
		super(gatewayMgr, moduleMgr, contractMgr, gatewayService, moduleService);
	}

	@Override
	public void start() {
		/// 准备工作 				
		MarketGateway mktGateway = createPlaybackGateway(ctx);
		TradeGateway tdGateway = createSimGateway(mktGateway);
		tdGateway.connect();
		gatewayService.simMoneyIO(tdGateway.gatewayId(), 100000);
		List<IModule> traineeModules = createModules(tdGateway);
		
		for(int i = 0; i < maxTrainingEpisodes(); i++) {
			/// 重置模组 				
			log.info("开始第{}个回合训练", i+1);
			traineeModules.forEach(this::enableModule);
			
			/// 复位历史回放
			gatewayService.resetPlayback(mktGateway.gatewayId());
			
			/// 数据预热 		
			mktGateway.connect();
			while (mktGateway.isActive()) {
				pause(1);
				log.info("数据预热中");
			}
			
			/// 开始回放
			traineeModules.forEach(m -> m.setEnabled(true));
			pause(1);
			mktGateway.connect();
			while (mktGateway.isActive()) {
				pause(30);
				log.info("第{}个回合训练中", i+1);
			}
			pause(30); // 等待计算结束

			log.info("第{}个回合训练结束", i+1);
		}
		
	}
	
	private void enableModule(IModule m) {
		try {
			moduleService.modifyModule(m.getModuleDescription(), true);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
	
	private List<IModule> createModules(TradeGateway tdGateway) {
		return testContracts().stream()
				.map(csi -> {
					ModuleAccountDescription mad = ModuleAccountDescription.builder()
							.accountGatewayId(tdGateway.gatewayId())
							.bindedContracts(List.of(csi))
							.build();
					ComponentAndParamsPair strategySettings = ComponentAndParamsPair.builder()
							.componentMeta(strategy())
							.initParams(convertParams(strategyParams(csi)))
							.build();
					return createModules(mad, strategySettings, csi);
				})
				.flatMap(List::stream)
				.toList();
	}
	
	private List<ContractSimpleInfo> testContracts(){
		return ctx.testSymbols().stream()
				.map(symbol -> symbol + "0000")
				.map(idxSymbol -> contractMgr.getContract(ChannelType.PLAYBACK, idxSymbol))
				.map(Contract::contractField)
				.map(cf -> ContractSimpleInfo.builder()
					.name(cf.getName())
					.channelType(ChannelType.PLAYBACK)
					.unifiedSymbol(cf.getUnifiedSymbol())
					.value(cf.getContractId())
					.build())
				.toList();
	}
	
	private List<IModule> createModules(ModuleAccountDescription mad, ComponentAndParamsPair strategySettings, ContractSimpleInfo csi){
		String symbolName = csi.getName();
		String symbol = csi.getUnifiedSymbol().replaceAll("\\d+.+$", "");
		return IntStream.of(testPeriods())
				.mapToObj(min -> ModuleDescription.builder()
						.moduleName(String.format("%s%d分钟", symbolName, min))
						.initBalance(ctx.symbolTestAmount().get(symbol))
						.usage(ModuleUsage.PLAYBACK)
						.type(ModuleType.SPECULATION)
						.closingPolicy(ClosingPolicy.FIRST_IN_FIRST_OUT)
						.numOfMinPerBar(min)
						.strategySetting(strategySettings)
						.moduleAccountSettingsDescription(List.of(mad))
						.build())
				.map(this::createModule)
				.map(md -> moduleMgr.get(Identifier.of(md.getModuleName())))
				.toList();
	}

	private MarketGateway createPlaybackGateway(ModuleTesterContext ctx) {
		String gatewayId = "历史回放_强化学习训练";
		List<ContractSimpleInfo> contracts = testContracts();
		
		JSONObject settings = new JSONObject();
		settings.put("preStartDate", ctx.preStartDate().format(DateTimeConstant.D_FORMAT_INT_FORMATTER));
		settings.put("startDate", ctx.startDate().format(DateTimeConstant.D_FORMAT_INT_FORMATTER));
		settings.put("endDate", ctx.endDate().format(DateTimeConstant.D_FORMAT_INT_FORMATTER));
		settings.put("precision", ctx.precision());
		settings.put("speed", ctx.speed());
		settings.put("playContracts", contracts);
		
		GatewayDescription gd = GatewayDescription.builder()
				.gatewayId(gatewayId)
				.gatewayUsage(GatewayUsage.MARKET_DATA)
				.channelType(ChannelType.PLAYBACK)
				.subscribedContracts(contracts)
				.settings(settings)
				.build();
		gatewayService.createGateway(gd);
		return (MarketGateway) gatewayMgr.get(Identifier.of(gatewayId));
	}
	
}
