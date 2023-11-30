package org.dromara.northstar.strategy;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.common.utils.FieldUtils;
import org.slf4j.Logger;

import com.alibaba.fastjson.JSONObject;

import lombok.Getter;
import lombok.Setter;

public abstract class AbstractStrategy implements TradeStrategy{
	
	// 模组计算状态
	@Getter
	@Setter
	protected JSONObject storeObject;
	// 模组上下文
	protected IModuleStrategyContext ctx;
	// 处理器，unifiedSymbol -> handler
	protected Map<String, TickHandler> tickHandlerMap = new HashMap<>();
	// 处理器，unifiedSymbol -> handler
	protected Map<String, BarHandler> barHandlerMap = new HashMap<>();
	// 日志对象
	protected Logger log;
	// 预热K线数据量（该预热数据量与模组的设置并不相等，该属性用于策略内部判断接收了多少数据，而模组的预热设置用于外部投喂了多少数据）
	protected int numOfBarsToPrepare;
	
	private Queue<Bar> barCache = new LinkedList<>();
	
	@Override
	public void onOrder(Order order) {
		// 如果策略不关心订单反馈，可以不重写
	}

	@Override
	public void onTrade(Trade trade) {
		// 如果策略不关心成交反馈，可以不重写
		String unifiedSymbol = trade.contract().unifiedSymbol();
		if(log.isInfoEnabled()) {
			log.info("模组成交 [{} {} {} 操作：{}{} {}手 {}]", unifiedSymbol,
					trade.tradeDate(), trade.tradeTime(), FieldUtils.chn(trade.direction()), FieldUtils.chn(trade.offsetFlag()), 
					trade.volume(), trade.price());
			log.info("当前模组净持仓：[{}]", ctx.getModuleAccount().getNonclosedNetPosition(unifiedSymbol));
			log.info("当前模组状态：{}", ctx.getState());
		}
	}

	@Override
	public void setContext(IModuleContext context) {
		ctx = context;
		log = ctx.getLogger();
		initIndicators();
		initMultiContractHandler();
	}
	
	public IModuleContext getContext() {
		return (IModuleContext) ctx;
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
	public void onTick(Tick tick) {
		if(!canProceed()) {
			return;
		}
		if(tickHandlerMap.containsKey(tick.contract().unifiedSymbol())) {
			tickHandlerMap.get(tick.contract().unifiedSymbol()).onTick(tick);
		}
	}
	
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
	public void onMergedBar(Bar bar) {
		if(!canProceed(bar)) {
			return;
		}
		if(barHandlerMap.containsKey(bar.contract().unifiedSymbol())) {
			barHandlerMap.get(bar.contract().unifiedSymbol()).onMergedBar(bar);
		}
	}
	
	protected boolean canProceed() {
		return barCache.isEmpty() && numOfBarsToPrepare == 0;
	}
	
	protected boolean canProceed(Bar bar) {
		if(barCache.size() < numOfBarsToPrepare) {
			if(barCache.isEmpty() || barCache.peek().contract().equals(bar.contract())) {
				barCache.offer(bar);
			}
			return false;
		}
		
		numOfBarsToPrepare = 0;
		barCache.clear();
		return true;
	}
	
	/**
	 * 订阅多个合约时，可以加上各自的处理器来减少if...else代码
	 * @param unifiedSymbol
	 * @param handler
	 */
	protected void addBarHandler(String unifiedSymbol, BarHandler handler) {
		barHandlerMap.put(unifiedSymbol, handler);
	}
	
	protected static interface TickHandler extends TickDataAware {}
	
	protected static interface BarHandler extends MergedBarListener {}
}
