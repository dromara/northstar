package org.dromara.northstar.strategy.trainer;

import java.util.List;
import java.util.stream.Collectors;

import org.dromara.northstar.common.IGatewayService;
import org.dromara.northstar.common.IModuleService;
import org.dromara.northstar.common.ObjectManager;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.constant.GatewayUsage;
import org.dromara.northstar.common.model.ComponentAndParamsPair;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.ModuleAccountDescription;
import org.dromara.northstar.gateway.Gateway;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.gateway.MarketGateway;
import org.dromara.northstar.gateway.TradeGateway;
import org.dromara.northstar.strategy.IModule;
import org.dromara.northstar.strategy.tester.ModuleTesterContext;

import com.alibaba.fastjson2.JSONObject;

import lombok.extern.slf4j.Slf4j;

/**
 * 串行训练器
 * @author KevinHuangwl
 *
 */
@Slf4j
public abstract class AbstractSerialTrainer extends AbstractTrainer implements RLAgentTrainingContext{

	protected AbstractSerialTrainer(ObjectManager<Gateway> gatewayMgr, ObjectManager<IModule> moduleMgr,
			IContractManager contractMgr, IGatewayService gatewayService, IModuleService moduleService) {
		super(gatewayMgr, moduleMgr, contractMgr, gatewayService, moduleService);
	}

	@Override
	public void start() {
		for(ContractSimpleInfo csi : testContracts()) {
			MarketGateway mktGateway = createPlaybackGateway(this, csi.getName());
			TradeGateway tdGateway = createSimGateway(mktGateway);
			String symbol = csi.getUnifiedSymbol().replaceAll("\\d+.+$", "");
			tdGateway.connect();
			gatewayService.simMoneyIO(tdGateway.gatewayId(), symbolTestAmount().get(symbol));
			ModuleAccountDescription mad = ModuleAccountDescription.builder()
					.accountGatewayId(tdGateway.gatewayId())
					.bindedContracts(List.of(csi))
					.build();
			ComponentAndParamsPair strategySettings = ComponentAndParamsPair.builder()
					.componentMeta(strategy())
					.initParams(convertParams(strategyParams(csi)))
					.build();
			createModules(mad, strategySettings, csi);
			
			for(int i = 0; i < maxTrainingEpisodes(); i++) {
				/// 重置模组 				
				log.info("【{}合约】 开始第{}个回合训练", csi.getName(), i+1);
				moduleMgr.findAll().forEach(this::resetModule);
				List<IModule> traineeModules = moduleMgr.findAll();
				
				/// 复位历史回放
				gatewayService.resetPlayback(mktGateway.gatewayId());
				mktGateway = (MarketGateway) gatewayMgr.get(Identifier.of(mktGateway.gatewayId()));
				
				/// 数据预热 		
				mktGateway.connect();
				while (mktGateway.isActive()) {
					log.info("数据预热中");
					pause(5);
				}
				pause(5 * traineeModules.size()); // 等待数据加载完成
				log.info("数据预热完成");

				/// 开始回放
				traineeModules.forEach(m -> m.setEnabled(true));
				pause(1);
				mktGateway.connect();
				while (mktGateway.isActive() && traineeModules.stream().anyMatch(IModule::isEnabled)) {
					pause(30);
					log.info("【{}合约】 第{}个回合训练中", csi.getName(), i+1);
				}
				pause(30); // 等待计算结束

				log.info("【{}合约】 第{}个回合训练结束", csi.getName(), i+1);
				if(!mrdMap.isEmpty()) {
					long numOfConverged = traineeModules.stream()
						.map(m -> m.getModuleContext().getRuntimeDescription(false))
						.filter(mrd -> hasPerformanceConverged(mrdMap.get(mrd.getModuleName()), mrd))
						.count();
					if(numOfConverged > traineeModules.size() / 2) {
						log.info("【{}合约】 模组总数为{}，其中有{}个模组已经收敛", csi.getName(), traineeModules.size(), numOfConverged);
						break;
					}
				}
				mrdMap = traineeModules.stream().collect(Collectors.toMap(IModule::getName, m -> m.getModuleContext().getRuntimeDescription(false)));
			}
			// 移除模组
			moduleMgr.findAll().stream().forEach(m -> moduleService.removeModule(m.getName()));
		}
	}

	private MarketGateway createPlaybackGateway(ModuleTesterContext ctx, String symbolName) {
		String gatewayId = "历史回放_" + symbolName;
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
	
	protected TradeGateway createSimGateway(MarketGateway mktGateway, String symbolName) {
		String gatewayId = "模拟账户_" + symbolName;
		GatewayDescription gd = GatewayDescription.builder()
				.gatewayId(gatewayId)
				.gatewayUsage(GatewayUsage.TRADE)
				.channelType(ChannelType.SIM)
				.bindedMktGatewayId(mktGateway.gatewayId())
				.settings(new JSONObject())
				.build();
		gatewayService.createGateway(gd);
		return (TradeGateway) gatewayMgr.get(Identifier.of(gatewayId));
	}
}
