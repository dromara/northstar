package tech.quantit.northstar.strategy.api;

import java.util.Map;
import java.util.Set;

import com.alibaba.fastjson.JSONObject;

import tech.quantit.northstar.common.TickDataAware;
import tech.quantit.northstar.common.TransactionAware;
import tech.quantit.northstar.strategy.api.indicator.Indicator;
import xyz.redtorch.pb.CoreField.ContractField;

public interface TradeStrategy extends TickDataAware, BarDataAware, TransactionAware, ContextAware{
	
	/* 状态与设置信息 */
	
	/**
	 * 绑定的（使用到的）指标集
	 * @return
	 */
	Map<String, Indicator> bindedIndicatorMap();
	/**
	 * 绑定的合约集
	 * @return
	 */
	Set<ContractField> bindedContracts();
	/**
	 * 获取计算状态
	 * @return
	 */
	JSONObject getComputedState();
	
	/* 响应接口 */
	
}
