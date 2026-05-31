package org.dromara.northstar.module;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.gateway.TradeGateway;
import org.dromara.northstar.strategy.IAccount;
import org.dromara.northstar.strategy.IModule;
import org.dromara.northstar.strategy.IModuleContext;
import org.slf4j.Logger;

import lombok.extern.slf4j.Slf4j;

/**
 * 交易模组
 * 主要负责模组状态组件管理、更新
 * 
 * @author KevinHuangwl
 *
 */
@Slf4j
public class TradeModule implements IModule {
	
	private final ModuleContext ctx;
	
	private final Set<String> bindedContractIdSet;
	
	private final Logger logger;
	
	public TradeModule(ModuleContext ctx) {
		this.ctx = ctx;
		logger = ctx.getLogger(getClass());
		bindedContractIdSet = ctx.getModuleDescription().getBindedContracts().stream().map(csi -> csi.getId()).collect(Collectors.toSet());
		logger.info("模组 [{}] 创建成功", ctx.getModuleDescription().getName());
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		ModuleAccount account = (ModuleAccount)ctx.getModuleAccount();
		if(enabled && account != null) {
			account.getContractAccountMap().values().stream().distinct().forEach(acc -> {
				TradeGateway gateway = acc.getTradeGateway();
				if(gateway.getConnection() != ConnectionState.CONNECTED) {
					log.warn("模组 [{}] 启动时，检测到账户 [{}] 没有连线，将自动连线", ctx.getModuleDescription().getName(), gateway.getDescription().getGatewayId());
					CompletableFuture.runAsync(gateway::connect);
				}
			});
		}
		ctx.setEnabled(enabled);
	}

	@Override
	public synchronized void onEvent(NorthstarEvent event) {
		Object data = event.getData();
		try {
			if(data instanceof Tick tick && bindedContractIdSet.contains(tick.contract().contractId())) {
				ctx.onTick(tick);
			} else if (data instanceof Bar bar && bindedContractIdSet.contains(bar.contract().contractId())) {
				ctx.onBar(bar);
			} else if (data instanceof Order order && bindedContractIdSet.contains(order.contract().contractId())) {
				ctx.onOrder(order);
			} else if (data instanceof Trade trade && bindedContractIdSet.contains(trade.contract().contractId())) {
				ctx.onTrade(trade);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	@Override
	public IModuleContext getModuleContext() {
		return ctx;
	}

	@Override
	public IAccount getModuleAccount() {
		return ctx.getModuleAccount();
	}

}
