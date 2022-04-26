package tech.quantit.northstar.domain.module;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import tech.quantit.northstar.strategy.api.IMarketDataStore;
import tech.quantit.northstar.strategy.api.IModuleContext;
import tech.quantit.northstar.strategy.api.TradeStrategy;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 行情数据储存器
 * @author KevinHuangwl
 *
 */
public class MarketDataStore implements IMarketDataStore {
	
	private TradeStrategy tradeStrategy;
	
	private IModuleContext ctx;
	
	private boolean enabled;
	
	private Set<String> bindedSymbols = new HashSet<>();

	/*
	 * 过滤行情数据
	 * */
	@Override
	public void onTick(TickField tick) {
		if(enabled && bindedSymbols.contains(tick.getUnifiedSymbol())) {
			ctx.onTick(tick);
		}
	}

	/*
	 * 过滤行情数据、合成多分钟K线
	 * */
	@Override
	public void onBar(BarField bar) {
		if(enabled && bindedSymbols.contains(bar.getUnifiedSymbol())) {
			tradeStrategy.bindedIndicatorMap()
				.values()
				.stream()
				.forEach(indicator -> indicator.onBar(bar));
			ctx.onBar(bar);
		}
	}

	@Override
	public void setContext(IModuleContext context) {
		tradeStrategy = context.getTradeStrategy();
		ctx = context;
	}

	@Override
	public void initWithBars(List<BarField> bars) {
		for(BarField bar : bars) {
			tradeStrategy.bindedIndicatorMap()
				.values()
				.stream()
				.forEach(indicator -> indicator.onBar(bar));
		}
	}

	@Override
	public void onModuleEnabledChange(boolean enabled) {
		this.enabled = enabled;
	}

}
