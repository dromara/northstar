package tech.quantit.northstar.domain.module;

import java.util.List;

import tech.quantit.northstar.common.event.NorthstarEvent;
import tech.quantit.northstar.common.model.ModuleDescription;
import tech.quantit.northstar.strategy.api.IModuleContext;
import tech.quantit.northstar.strategy.api.TradeStrategy;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

/*
 * 难点：
 * 1. 模组启停状态，如何能既不影响行情数据更新，又能限制指令发送	DONE（通过MarketDataStore控制）
 * 2. 外部需要观察模组当前运行的各种状态，要如何封装描述 		DONE（通过ModuleDescription封装）
 * 3. 模组可能存在多个交易所，根据合约价差在多个交易所下单		DONE（多个交易所网关在context中保存，由于策略下单时指定）
 * 4. 策略、风控、下单模块怎么整合
 * 
 * */

/**
 * 交易模组
 * 主要负责模组状态组件管理、更新
 * 
 * @author KevinHuangwl
 *
 */
public class TradeModule implements IModule {
	
	private String name;
	
	private boolean enabled;
	
	private IMarketDataStore mktDataStore;
	
	private IModuleAccountStore accStore;
	
	private IModuleOrderingStore orderStore;
	
	private IModuleContext ctx;
	
	private TradeStrategy strategy;
	
	public TradeModule(String name, IMarketDataStore marketDataStore, IModuleAccountStore accountStore, IModuleOrderingStore orderStore,
			IModuleContext context, TradeStrategy strategy) {
		this.name = name;
		this.mktDataStore = marketDataStore;
		this.accStore = accountStore;
		this.orderStore = orderStore;
		this.ctx = context;
		this.strategy = strategy;
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setEnabled(boolean enabled) {
		mktDataStore.setModuleEnabled(enabled);
		this.enabled = enabled;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void initModule() {
		mktDataStore.addEnabledToggleCallback(flag -> this.enabled = flag);
		ctx.setComponent(mktDataStore);
		ctx.setComponent(accStore);
		ctx.setComponent(orderStore);
		ctx.setTradeStrategy(strategy);
	}
	
	@Override
	public void initData(List<BarField> historyBars) {
		mktDataStore.initWithBars(historyBars);
	}

	@Override
	public void onEvent(NorthstarEvent event) {
		Object data = event.getData();
		if(data instanceof TickField tick) {
			mktDataStore.onTick(tick);
		} else if (data instanceof BarField bar) {
			mktDataStore.onBar(bar);
		} else if (data instanceof OrderField order) {
			orderStore.onOrder(order);
			accStore.onOrder(order);
		} else if (data instanceof TradeField trade) {
			orderStore.onTrade(trade);
			accStore.onTrade(trade);
		}
	}


	@Override
	public ModuleDescription getModuleDescription() {
		ModuleDescription md = ctx.getModuleDescription();
		md.setEnabled(enabled);
		md.setModuleName(name);
		md.setDataState(strategy.getComputedState());
		return md;
	}

}
