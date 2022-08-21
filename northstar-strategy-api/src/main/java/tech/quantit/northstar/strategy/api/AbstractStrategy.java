package tech.quantit.northstar.strategy.api;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.slf4j.Logger;

import com.alibaba.fastjson.JSONObject;

import tech.quantit.northstar.common.TickDataAware;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

public abstract class AbstractStrategy implements TradeStrategy{
	
	// 模组上下文
	protected IModuleStrategyContext ctx;
	// 模组计算状态
	protected JSONObject inspectableState;
	// 处理器，unifiedSymbol -> handler
	protected Map<String, TickHandler> tickHandlerMap = new HashMap<>();
	// 处理器，unifiedSymbol -> handler
	protected Map<String, BarHandler> barHandlerMap = new HashMap<>();
	// 日志对象
	protected Logger log;
	// 预热K线数据量（该预热数据量与模组的设置并不相等，该属性用于策略内部判断接收了多少数据，而模组的预热设置用于外部投喂了多少数据）
	protected int numOfBarsToPrepare;
	
	private Queue<BarField> barCache = new LinkedList<>();
	
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
		ctx = context;
		log = ctx.getLogger();
		initIndicators();
		initMultiContractHandler();
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
	 * 指标初始化
	 */
	protected void initIndicators() {}
	
	/**
	 * 多合约处理器初始化
	 */
	protected void initMultiContractHandler() {}

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
		if(!canProceed(bar)) {
			return;
		}
		if(barHandlerMap.containsKey(bar.getUnifiedSymbol()) && isModuleEnabled) {
			barHandlerMap.get(bar.getUnifiedSymbol()).onBar(bar);
		} else if(isModuleEnabled) {
			onBar(bar);
		}
	}
	
	protected boolean canProceed(BarField bar) {
		if(barCache.size() < numOfBarsToPrepare) {
			if(barCache.isEmpty() || barCache.peek().getUnifiedSymbol().equals(bar.getUnifiedSymbol())) {
				barCache.offer(bar);
			}
			return false;
		}
		numOfBarsToPrepare = 0;
		barCache.clear();
		return true;
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
	
	protected static interface TickHandler extends TickDataAware {}
	
	protected static interface BarHandler extends BarDataAware {}
}
