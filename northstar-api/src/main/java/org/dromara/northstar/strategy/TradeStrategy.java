package org.dromara.northstar.strategy;

import java.util.List;

import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.common.TransactionAware;
import org.dromara.northstar.common.constant.ModuleType;
import org.dromara.northstar.common.model.ItemDescription;

import com.alibaba.fastjson.JSONObject;

import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

public interface TradeStrategy extends TickDataAware, MergedBarListener, TransactionAware, ContextAware, DynamicParamsAware{
	
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
	void onTick(TickField tick);
	/**
	 * BAR事件
	 * 当模组状态为停用时，也不排除策略会有相应的数据更新逻辑，所以即使模组状态为停用，该方法仍会被调用
	 * @param bar
	 */
	void onMergedBar(BarField bar);
	/**
	 * 获取策略信息
	 * @return
	 */
	default List<ItemDescription> strategyInfos() {
		return List.of();
	}
}
