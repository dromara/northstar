package org.dromara.northstar.support.job;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dromara.northstar.account.AccountManager;
import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.ModuleAccountDescription;
import org.dromara.northstar.event.IllegalOrderHandler;
import org.dromara.northstar.gateway.Contract;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.module.ModuleManager;
import org.dromara.northstar.strategy.IAccount;
import org.dromara.northstar.strategy.IModule;
import org.dromara.northstar.support.notification.IMessageSenderManager;
import org.dromara.northstar.support.utils.ExceptionLogChecker;
import org.dromara.northstar.support.utils.InetAddressUtils;
import org.dromara.northstar.support.utils.PositionChecker;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreField.PositionField;

/**
 * 模组运行时检查任务
 * @author KevinHuangwl
 *
 */
@Slf4j
@Component
public class ModuleCheckingTask implements InitializingBean {
	
	@Autowired
	private IContractManager contractMgr;

	@Autowired
	private ModuleManager moduleMgr;
	
	@Autowired
	private AccountManager accountMgr;
	
	@Autowired
	private IMessageSenderManager msgMgr;
	
	@Autowired
	private IllegalOrderHandler illOrderHandler;
	
	private PositionChecker posChkr;
	
	private Map<IModule, ModuleState> moduleStateMap = new HashMap<>();
	
	private Set<String> warningCacheSet = new HashSet<>();
	
	/**
	 * 检查模组状态
	 * 如果模组状态不等于期望状态，则等待复查
	 * 周一至周五，每隔一小时，在整点检查一次
	 */
	@Scheduled(cron="0 0 0/1 ? * 1-5")
	public void checkModuleState() {
		log.debug("检查模组状态");
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
		log.debug("模组状态复查");
		moduleMgr.allModules().forEach(m -> {
			ModuleState state = m.getModuleContext().getState();
			if(!moduleStateMap.containsKey(m)) {
				return;
			}
			if(moduleStateMap.get(m).equals(state)) {
				msgMgr.getSubscribers().forEach(sub -> 
					doSmartSend(sub, "[模组状态警报]：" + m.getName(), String.format("当前模组状态为：%s，已经维持了一段时间，请人为介入判断是否正常", state))
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
		log.debug("核对模组的逻辑持仓与物理持仓是否一致");
		moduleMgr.allModules().forEach(m -> {
			for(ModuleAccountDescription mad : m.getModuleDescription().getModuleAccountSettingsDescription()) {
				for(ContractSimpleInfo csi : mad.getBindedContracts()) {
					Contract contract = contractMgr.getContract(Identifier.of(csi.getValue()));
					IAccount account = m.getAccount(contract);
					account.getPosition(PositionDirectionEnum.PD_Long, csi.getUnifiedSymbol()).ifPresent(this::doCheckPosition);
					account.getPosition(PositionDirectionEnum.PD_Short, csi.getUnifiedSymbol()).ifPresent(this::doCheckPosition);
				}
			}
		});
	}
	
	private void doCheckPosition(PositionField position) {
		try {
			posChkr.checkPositionEquivalence(position);
		} catch (IllegalStateException e) {
			msgMgr.getSubscribers().forEach(sub -> 
				doSmartSend(sub, String.format("[持仓不匹配警报] %s %s", position.getContract().getName(), position.getPositionDirection()), e.getMessage())
			);
		}
	}
	
	/**
	 * 检查当天的模组日志中是否存在异常日志，如存在则转发报告
	 * 周一至五，每隔一小时检查 
	 * @throws IOException 
	 */
	@Scheduled(cron="0 0 0/1 ? * 1-5")
	public void checkModuleException() throws IOException {
		log.debug("检查当天的模组日志中是否存在异常日志");
		for(IModule module : moduleMgr.allModules()) {
			Logger logger = (Logger) module.getModuleContext().getLogger();
			FileAppender<ILoggingEvent> fileAppender = (FileAppender<ILoggingEvent>) logger.getAppender(module.getName());
			File logFile = new File(fileAppender.getFile());
			FileReader fr = new FileReader(logFile);
			ExceptionLogChecker checker = new ExceptionLogChecker(fr);
			LocalTime endTime = LocalTime.now();
			LocalTime startTime = endTime.minusHours(1);
			List<String> errorLines = checker.getExceptionLog(startTime, endTime);
			if(!errorLines.isEmpty()) {
				StringBuilder sb = new StringBuilder();
				errorLines.forEach(line -> sb.append(line + "%n"));
				msgMgr.getSubscribers().forEach(sub -> 
					doSmartSend(sub, String.format("[模组异常日志警报] %s %d-%d时，%d条异常记录", module.getName(),
							startTime.getHour(), endTime.getHour(), errorLines.size()), sb.toString())
				);
			}
		}
	}
	/**
	 * 检查风险度，超过95%则发出警报
	 * 每两小时检查一次
	 */
	@Scheduled(cron="0 0 0/2 ? * 1-5")
	public void checkDegreeOfRisk() {
		log.debug("检查风险度");
		accountMgr.allAccounts().stream()
			.filter(account -> account.degreeOfRisk() > 0.95)
			.forEach(account -> 
				msgMgr.getSubscribers().forEach(sub -> 
					doSmartSend(sub, "[账户风险度警报]：" + account.accountId(), String.format("当前账户风险率为：%d%", (int)(account.degreeOfRisk() * 100)))
				)
			);
	}
	/**
	 * 检查废单
	 */
	@Scheduled(cron="0 0 0/1 ? * 1-5")
	public void checkIllegalOrder() {
		log.debug("检查废单");
		StringBuilder sb = new StringBuilder();
		illOrderHandler.getIllegalOrders().forEach(order -> 
			sb.append(String.format("%s %s %s %s%n", order.getContract().getName(), order.getOrderDate(), order.getOrderTime(), order.getStatusMsg()))
		);
		
		if(sb.length() > 0) {
			msgMgr.getSubscribers().forEach(sub -> 
				doSmartSend(sub, "[当天废单列表警报]", sb.toString())
			);
		}
	}
	
	private void doSmartSend(String subscriber, String title, String msg) {
		String combine = title + "@" + msg;
		if(!warningCacheSet.contains(combine)) {
			String realMsg = String.format("%s%n%n警报来源：%s%n", msg, InetAddressUtils.getHostname());
			msgMgr.getSender().send(subscriber, title, realMsg);
			warningCacheSet.add(combine);
		}
	}
	
	/**
	 * 清除报警缓存
	 */
	@Scheduled(cron="0 45 8,12,20 ? * 1-5")
	public void resetWarningCache() {
		log.debug("清除报警缓存");
		warningCacheSet.clear();
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		this.posChkr = new PositionChecker(moduleMgr);
	}
}
