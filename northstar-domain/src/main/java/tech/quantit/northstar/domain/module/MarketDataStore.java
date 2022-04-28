package tech.quantit.northstar.domain.module;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import tech.quantit.northstar.strategy.api.IMarketDataStore;
import tech.quantit.northstar.strategy.api.IModuleContext;
import tech.quantit.northstar.strategy.api.TradeStrategy;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;
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
	/* unifiedSymbol -> barMerger */
	private Map<String, BarMerger> contractBarMergerMap = new HashMap<>();
	
	public MarketDataStore(int n) {
		Consumer<BarField> callback = bar -> {
			tradeStrategy.bindedIndicatorMap()
				.values()
				.stream()
				.forEach(indicator -> indicator.onBar(bar));
			ctx.onBar(bar);
		};
		for(ContractField contract : tradeStrategy.bindedContracts()) {
			contractBarMergerMap.put(contract.getUnifiedSymbol(), new BarMerger(n, contract, callback));
		}
	}

	/*
	 * 过滤行情数据
	 * */
	@Override
	public void onTick(TickField tick) {
		if(enabled && contractBarMergerMap.containsKey(tick.getUnifiedSymbol())) {
			ctx.onTick(tick);
		}
	}

	/*
	 * 过滤行情数据、合成多分钟K线
	 * */
	@Override
	public void onBar(BarField bar) {
		if(enabled && contractBarMergerMap.containsKey(bar.getUnifiedSymbol())) {
			contractBarMergerMap
				.get(bar.getUnifiedSymbol())
				.updateBar(bar);
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
			if(contractBarMergerMap.containsKey(bar.getUnifiedSymbol())) {
				contractBarMergerMap
					.get(bar.getUnifiedSymbol())
					.updateBar(bar);
			}
		}
	}

	@Override
	public void onModuleEnabledChange(boolean enabled) {
		this.enabled = enabled;
	}

}
