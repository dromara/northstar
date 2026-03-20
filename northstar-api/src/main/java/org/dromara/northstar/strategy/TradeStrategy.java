package org.dromara.northstar.strategy;

import java.util.Collections;
import java.util.List;

import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.common.TransactionAware;
import org.dromara.northstar.common.constant.ModuleType;
import org.dromara.northstar.common.model.ListValue;
import org.dromara.northstar.common.model.ModuleIndicatorDescription;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Tick;

import com.alibaba.fastjson2.JSONObject;

public interface TradeStrategy extends TickDataAware, MergedBarListener, TransactionAware, ContextAware, DynamicParamsAware {
	
	/* 状态与设置信息 */
	/**
	 * 适用模组类型
	 * @return
	 */
	default ModuleType type() {
		return ModuleType.SPECULATION;
	}
	
	/**
	 * 获取计算状态
	 * @return
	 */
	JSONObject getStoreObject();
	/**
	 * 设置计算状态
	 * @param storeObj
	 */
	void setStoreObject(JSONObject storeObj);
	
	/* 响应事件 */
	/**
	 * TICK事件
	 * 当模组状态为停用时，也不排除策略会有相应的数据更新逻辑，所以即使模组状态为停用，该方法仍会被调用
	 * @param tick
	 */
	void onTick(Tick tick);
	/**
	 * BAR事件
	 * 当模组状态为停用时，也不排除策略会有相应的数据更新逻辑，所以即使模组状态为停用，该方法仍会被调用
	 * @param bar
	 */
	void onMergedBar(Bar bar);
	/**
	 * 获取策略信息
	 * 该方法原本用于在网页监控台提供一些策略的信息以便于监控
	 * 每个ListValue可以理解为一个面板，其能包含多个值
	 * 
	 * @apiNote	该接口属于高级用法，新手可以忽略
	 * @see	策略案例可以参考CustomVisibilityExampleStrategy
	 */
	default List<ListValue> strategyInfos() {
		return Collections.emptyList();
	}
	/**
	 * 增补指标信息
	 * 该方法提供的数据，将会以指标线的方式展示。
	 * 由于指标框架的指标线不能修改其历史值，部分指标例如缠论、ZigZag等需要修复最近的值来更新指标显示，该接口相当于给用户提供了一个独立于指标框架的补充手段
	 * 用法：
	 * 每个ModuleIndicatorVO对象一个自定义的指标数据
	 * 
	 * @apiNote	该接口属于高级用法，新手可以忽略
	 * @see	策略案例可以参考CustomVisibilityExampleStrategy
	 */
	default List<ModuleIndicatorDescription> customIndicators(){
		return Collections.emptyList();
	}
	
	/**
	 * 策略名称
	 * @return
	 */
	String name();
}
