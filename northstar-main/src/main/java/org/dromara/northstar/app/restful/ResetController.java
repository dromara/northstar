package org.dromara.northstar.app.restful;

import org.dromara.northstar.app.service.ContractConfigService;
import org.dromara.northstar.app.service.CustomIndicatorService;
import org.dromara.northstar.app.service.GatewayService;
import org.dromara.northstar.app.service.ModuleService;
import org.dromara.northstar.app.rl.RLTrainingService;
import org.dromara.northstar.common.constant.GatewayUsage;
import org.dromara.northstar.event.SseBroadcastHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Profile({"e2e", "dev"})
@RestController
@RequestMapping("northstar/reset")
public class ResetController {
	
	@Autowired
	private GatewayService gwService;
	
	@Autowired
	private ModuleService moduleService;
	
	@Autowired
	private SseBroadcastHandler broadcastHandler;
	
	@Autowired
	private CustomIndicatorService customIndicatorService;
	
	@Autowired
	private ContractConfigService contractConfigService;
	
	@Autowired
	private RLTrainingService rlTrainingService;

	@DeleteMapping
	public void reset() {
		log.info("重置程序");
		// 停止并清空 RL 训练任务
		rlTrainingService.resetAll();
		// 移除模组
		moduleService.getModules().stream()
			.forEach(m -> moduleService.removeModule(m.getDescription().getName()));
		// 全部断开
		gwService.findAllGateway().stream()
			.forEach(gw -> gwService.disconnect(gw.getDescription().getGatewayId()));
		// 移除交易网关
		gwService.findAllGateway().stream()
			.filter(gw -> gw.getDescription().getGatewayUsage() == GatewayUsage.TRADE)
			.forEach(gw -> 
				gwService.removeGateway(gw.getDescription().getGatewayId())
			);
		// 移除行情网关
		gwService.findAllGateway().stream()
			.filter(gw -> gw.getDescription().getGatewayUsage() == GatewayUsage.MARKET_DATA)
			.forEach(gw -> 
				gwService.removeGateway(gw.getDescription().getGatewayId())
			);
		// 移除自定义指标
		customIndicatorService.findAll().stream()
			.forEach(ind -> customIndicatorService.delete(ind.getId()));
		
		// 移除合约配置
		contractConfigService.findAll("", true).stream().forEach(vo -> contractConfigService.resetToDefault(vo.getContractId()));
		
		broadcastHandler.resetCache();
	}
}
