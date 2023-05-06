package org.dromara.northstar.support.job;

import java.util.HashMap;
import java.util.Map;

import org.dromara.northstar.account.AccountManager;
import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.event.IllegalOrderHandler;
import org.dromara.northstar.module.ModuleManager;
import org.dromara.northstar.strategy.IModule;
import org.dromara.northstar.support.notification.IMessageSenderManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 模组运行时检查任务
 * @author KevinHuangwl
 *
 */

@Component
public class ModuleCheckingTask {

	@Autowired
	private ModuleManager moduleMgr;
	
	@Autowired
	private AccountManager accountMgr;
	
	@Autowired
	private IMessageSenderManager msgMgr;
	
	@Autowired
	private IllegalOrderHandler illOrderHandler;
	
	private Map<IModule, ModuleState> moduleStateMap = new HashMap<>();
	
	/**
	 * 检查模组状态
	 * 如果模组状态不等于期望状态，则等待复查
	 * 周一至周五，每隔一小时，在整点检查一次
	 */
	@Scheduled(cron="0 0 0/1 ? * 1-5")
	public void checkModuleState() {
		moduleMgr.allModules().forEach(m -> {
			ModuleState state = m.getModuleContext().getState();
			if(state.isEmpty() || state.isHolding()) {
				return;
			}
			moduleStateMap.put(m, state);
		});
	}
	/**
	 * 模组状态复查
	 * 如果模组复查状态仍不等于期望状态，则发出警报，待人为介入
	 * 周一至周五，每隔一小时，在15分时检查一次
	 */
	@Scheduled(cron="0 15 0/1 ? * 1-5")
	public void doubleCheckModuleState() {
		moduleMgr.allModules().forEach(m -> {
			ModuleState state = m.getModuleContext().getState();
			if(moduleStateMap.get(m).equals(state)) {
				msgMgr.getSubscribers().forEach(sub -> 
					msgMgr.getSender().send(sub, "[模组状态警报]：" + m.getName(), String.format("当前模组状态为：%s，已经维持了一段时间，请人为介入判断是否正常", state))
				);
			} else {
				moduleStateMap.remove(m);
			}
		});
	}
	/**
	 * 核对模组的逻辑持仓与物理持仓是否一致
	 * 若不一致，则发出警报，待人为介入
	 */
	@Scheduled(cron="0 0 0/1 ? * 1-5")
	public void checkHoldingLogicalAndPhysical() {
		
	}
	/**
	 * 检查当天的程序日志中是否存在异常日志，如存在则转发报告
	 */
	@Scheduled(cron="0 0/1 0-1,9-14,21-23 ? * 1-5")
	public void checkAppException() {
		
	}
	/**
	 * 检查当天的模组日志中是否存在异常日志，如存在则转发报告
	 */
	@Scheduled(cron="0 0/1 0-1,9-14,21-23 ? * 1-5")
	public void checkModuleException() {
		
	}
	/**
	 * 检查风险度，超过95%则发出警报
	 * 每两小时检查一次
	 */
	@Scheduled(cron="0 0 0/2 ? * 1-5")
	public void checkDegreeOfRisk() {
		accountMgr.allAccounts().stream()
			.filter(account -> account.degreeOfRisk() > 0.95)
			.forEach(account -> 
				msgMgr.getSubscribers().forEach(sub -> 
					msgMgr.getSender().send(sub, "[账户风险度警报]：" + account.accountId(), String.format("当前账户风险率为：%d%", (int)(account.degreeOfRisk() * 100)))
				)
			);
	}
	/**
	 * 检查废单
	 */
	@Scheduled(cron="0 0 0/1 ? * 1-5")
	public void checkIllegalOrder() {
		StringBuilder sb = new StringBuilder();
		illOrderHandler.getIllegalOrders().forEach(order -> 
			sb.append(String.format("%s %s %s %s%n", order.getContract().getName(), order.getOrderDate(), order.getOrderTime(), order.getStatusMsg()))
		);
		
		if(sb.length() > 0) {
			msgMgr.getSubscribers().forEach(sub -> 
				msgMgr.getSender().send(sub, "[当天废单列表警报]", sb.toString())
			);
		}
	}
}
