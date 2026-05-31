package org.dromara.northstar.strategy;

import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.common.utils.FieldUtils;
import org.slf4j.Logger;

import com.alibaba.fastjson2.JSONObject;

import lombok.Getter;
import lombok.Setter;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

public abstract class AbstractStrategy implements TradeStrategy{
	
	// 模组计算状态
	@Getter
	@Setter
	protected JSONObject storeObject;
	// 模组上下文
	protected IModuleStrategyContext ctx;
	// 日志对象
	protected Logger logger;
	// 预热K线数据量（该预热数据量与模组的设置并不相等，该属性用于策略内部判断接收了多少数据，而模组的预热设置用于外部投喂了多少数据）
	protected int numOfBarsToPrepare;
	
	@Override
	public void onOrder(Order order) {
		// 如果策略不关心订单反馈，可以不重写
	}

	@Override
	public void onTrade(Trade trade) {
		// 如果策略不关心成交反馈，可以不重写
		if(logger.isInfoEnabled()) {
			IAccount moduleAcc = ctx.getModuleAccount();
			Contract c = trade.contract();
			logger.info("模组成交 [{} {} {} 操作：{}{} 数量：{} 价格：{}]", trade.contract().unifiedSymbol(),
					trade.tradeDate(), trade.tradeTime(), FieldUtils.chn(trade.direction()), FieldUtils.chn(trade.offsetFlag()), 
					trade.volume(), trade.price());
			logger.info("当前模组持仓：多头[{}] 空头[{}]", moduleAcc.getPosition(PositionDirectionEnum.PD_Long, c), moduleAcc.getPosition(PositionDirectionEnum.PD_Short, c));
			logger.info("当前模组状态：{}", ctx.getState());
		}
	}

	/* 该方法不应该被重写，但可以扩展 */
	@Override
	public void setContext(IModuleContext context) {
		ctx = context;
		logger = ctx.getLogger(getClass());
		initIndicators();
		initMultiContractHandler();
	}
	
	/**
	 * 是否启用
	 * @return
	 */
	public boolean isEnabled() {
		return ctx.isEnabled();
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
	public void onTick(Tick tick) {}
	
	/**
	 * 该方法不管模组是否启用都会被调用
	 * 每个K线触发一次
	 * 如果订阅了多个合约，则会有多个K线，因此每个K线收盘时刻会触发多次
	 */
	@Override
	public void onMergedBar(Bar bar) {}
	
	public static interface TickHandler extends TickDataAware {}
	
	public static interface BarHandler extends MergedBarListener {}
}
