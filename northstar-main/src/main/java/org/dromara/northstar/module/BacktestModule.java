package org.dromara.northstar.module;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.model.ModuleChartDescription;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.indicator.constant.PeriodUnit;
import org.dromara.northstar.strategy.IAccount;
import org.dromara.northstar.strategy.IModule;
import org.dromara.northstar.strategy.IModuleContext;
import org.slf4j.Logger;

public class BacktestModule implements IModule{
	
	private ModuleContext ctx;
	
	private Logger logger;
	
	private AtomicInteger barCounter = new AtomicInteger();
	
	private Consumer<ModuleChartDescription> chartCallbackHandler;
	
	public BacktestModule(ModuleContext ctx) {
		this.ctx = ctx;
		ctx.setModule(this);;
		this.logger = ctx.getLogger(getClass());
		
		Contract c = ctx.bindedContracts().getFirst();
		ctx.addMergedBarListener(c, ctx.numOfMinPerMergedBar(), PeriodUnit.MINUTE, bar -> {
			if(barCounter.incrementAndGet() == BacktestModuleFactory.BUF_SIZE) {
				barCounter.set(0);
				if(chartCallbackHandler != null) {
					chartCallbackHandler.accept(ctx.getChartDescription());
				}
			}
		});
	}
	
	public void addListener(Consumer<ModuleChartDescription> chartCallbackHandler) {
		this.chartCallbackHandler = chartCallbackHandler;
	}

	@Override
	public void setEnabled(boolean enabled) {
		ctx.setEnabled(enabled);
	}

	@Override
	public void onEvent(NorthstarEvent event) {
		Object data = event.getData();
		if(!ctx.isReady()) {
			return;
		}
		try {
			if(data instanceof Tick tick) {
				ctx.onTick(tick);
			} else if (data instanceof Bar bar) {
				ctx.onBar(bar);
			} else if (data instanceof Order order) {
				ctx.onOrder(order);
			} else if (data instanceof Trade trade) {
				ctx.onTrade(trade);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public IAccount getAccount(Contract contract) {
		throw new UnsupportedOperationException("在策略探索模式下，没有绑定任何物理账户信息，不支持相关的操作");
	}

	@Override
	public IModuleContext getModuleContext() {
		return ctx;
	}

}
