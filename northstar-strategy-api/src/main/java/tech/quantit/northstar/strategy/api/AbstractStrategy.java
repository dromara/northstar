package tech.quantit.northstar.strategy.api;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;

import tech.quantit.northstar.common.TickDataAware;
import tech.quantit.northstar.strategy.api.indicator.Indicator;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

public abstract class AbstractStrategy implements TradeStrategy{
	
	// 模组上下文
	protected IModuleContext ctx;
	// 模组计算状态
	protected JSONObject inspectableState;
	// 指标集，name -> indicator
	protected Map<String, Indicator> indicatorMap = new HashMap<>();
	// 处理器，unifiedSymbol -> handler
	protected Map<String, TickHandler> tickHandlerMap = new HashMap<>();
	// 处理器，unifiedSymbol -> handler
	protected Map<String, BarHandler> barHandlerMap = new HashMap<>();
	
	@Override
	public void onOrder(OrderField order) {
		// 如果策略不关心订单反馈，可以不重写
	}

	@Override
	public void onTrade(TradeField trade) {
		// 如果策略不关心成交反馈，可以不重写
	}

	@Override
	public void setContext(IModuleContext context) {
		this.ctx = context;
	}

	@Override
	public Map<String, Indicator> bindedIndicatorMap() {
		return indicatorMap;
	}

	@Override
	public JSONObject getComputedState() {
		return inspectableState;
	}

	@Override
	public void setComputedState(JSONObject stateObj) {
		this.inspectableState = stateObj;
	}

	/**
	 * 该方法不管模组是否启用都会被调用
	 * 每个TICK触发一次
	 * 如果订阅了多个合约，则会有多个TICK，因此每个TICK时刻会触发多次
	 */
	@Override
	public void onTick(TickField tick, boolean isModuleEnabled) {
		if(tickHandlerMap.containsKey(tick.getUnifiedSymbol()) && isModuleEnabled) {
			tickHandlerMap.get(tick.getUnifiedSymbol()).onTick(tick);
		} else if(isModuleEnabled) {
			onTick(tick);
		}
	}
	
	/**
	 * 该方法旨在简化代码，与以上的onTick方法二选一，进行重写 
	 */
	protected void onTick(TickField tick) {}
	
	/**
	 * 订阅多个合约时，可以加上各自的处理器来减少if...else代码
	 * @param unifiedSymbol
	 * @param handler
	 */
	protected void addTickHandler(String unifiedSymbol, TickHandler handler) {
		tickHandlerMap.put(unifiedSymbol, handler);
	}

	/**
	 * 该方法不管模组是否启用都会被调用
	 * 每个K线触发一次
	 * 如果订阅了多个合约，则会有多个K线，因此每个K线时刻会触发多次
	 */
	@Override
	public void onBar(BarField bar, boolean isModuleEnabled) {
		if(barHandlerMap.containsKey(bar.getUnifiedSymbol()) && isModuleEnabled) {
			barHandlerMap.get(bar.getUnifiedSymbol()).onBar(bar);
		} else if(isModuleEnabled) {
			onBar(bar);
		}
	}
	
	/**
	 * 该方法旨在简化代码，与以上的onTick方法二选一，进行重写 
	 * @param bar
	 */
	protected void onBar(BarField bar) {}

	/**
	 * 订阅多个合约时，可以加上各自的处理器来减少if...else代码
	 * @param unifiedSymbol
	 * @param handler
	 */
	protected void addBarHandler(String unifiedSymbol, BarHandler handler) {
		barHandlerMap.put(unifiedSymbol, handler);
	}
	
	static interface TickHandler extends TickDataAware {}
	
	static interface BarHandler extends BarDataAware {}
}
