package tech.quantit.northstar.strategy.api;

import java.util.Map;

import com.alibaba.fastjson.JSONObject;

import tech.quantit.northstar.common.TransactionAware;
import tech.quantit.northstar.strategy.api.indicator.Indicator;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

public interface TradeStrategy extends TransactionAware, ContextAware, DynamicParamsAware{
	
	/* 状态与设置信息 */
	
	/**
	 * 绑定的（使用到的）指标集
	 * @return
	 */
	Map<String, Indicator> bindedIndicatorMap();
	/**
	 * 获取计算状态
	 * @return
	 */
	JSONObject getComputedState();
	/**
	 * 设置计算状态
	 * @param stateObj
	 */
	void setComputedState(JSONObject stateObj);
	
	/* 响应事件 */
	/**
	 * TICK事件
	 * @param tick
	 * @param isModuleEnabled
	 */
	void onTick(TickField tick, boolean isModuleEnabled);
	/**
	 * BAR事件
	 * @param bar
	 * @param isModuleEnabled
	 */
	void onBar(BarField bar, boolean isModuleEnabled);
}
