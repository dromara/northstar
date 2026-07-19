package org.dromara.northstar.module;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.gateway.TradeGateway;
import org.dromara.northstar.kline.KlineGenerator;
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
	
	private final Map<Contract, KlineGenerator> generatorMap;
	
	private final Logger logger;
	
	public TradeModule(ModuleContext ctx) {
		this.ctx = ctx;
		logger = ctx.getLogger(getClass());
		generatorMap = ctx.bindedContracts().stream().collect(Collectors.toMap(c -> c, c -> new KlineGenerator(c, ctx::onBar)));
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
			if(data instanceof Tick tick && generatorMap.containsKey(tick.contract())) {
				ctx.onTick(tick);
				generatorMap.get(tick.contract()).updateTick(tick);
			} else if (data instanceof Bar bar && generatorMap.containsKey(bar.contract()) && bar.interval() == Interval.M1) {
				generatorMap.get(bar.contract()).updateBar(bar);
			} else if (data instanceof Order order && generatorMap.containsKey(order.contract())) {
				ctx.onOrder(order);
			} else if (data instanceof Trade trade && generatorMap.containsKey(trade.contract())) {
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

	@Override
	public KlineGenerator getKlineGenerator(Contract contract) {
		return generatorMap.get(contract);
	}

}
