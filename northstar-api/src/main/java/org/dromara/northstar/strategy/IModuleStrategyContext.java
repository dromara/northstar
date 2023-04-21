package org.dromara.northstar.strategy;

import java.util.function.Function;

import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.model.BarWrapper;
import org.dromara.northstar.common.model.TimeSeriesValue;
import org.dromara.northstar.strategy.constant.DisposablePriceListenerType;
import org.dromara.northstar.strategy.model.Indicator;
import org.dromara.northstar.strategy.model.Indicator.ValueType;
import org.dromara.northstar.strategy.model.TradeIntent;
import org.slf4j.Logger;

import com.google.common.util.concurrent.AtomicDouble;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TradeField;

public interface IModuleStrategyContext {
	/**
	 * 为条件添加日志解释
	 * @param expression		判断条件
	 * @param infoMessage		条件成立时的日志输出
	 * @param args				条件成立时的日志参数
	 * @return					条件真假
	 */
	boolean explain(boolean expression, String infoMessage, Object... args);
	/**
	 * 获取合约
	 * @param unifiedSymbol		合约编码
	 * @return					返回合约信息
	 */
	ContractField getContract(String unifiedSymbol);
	/**
	 * 委托下单（根据配置自动处理撤单追单）
	 * @param tradeIntent		交易意图
	 */
	void submitOrderReq(TradeIntent tradeIntent);
	/**
	 * 止损止盈操作，达到价位会自动触发平仓逻辑
	 * 自动失效条件：
	 * 1. 触发一次后失效
	 * 2. 模组净持仓为零时
	 * 可通过 DisposablePriceListener.invalidate 方法主动失效
	 * 【注意】：止盈止损单没有实现状态持仓化，程序重启会导致其丢失，注意处理
	 * @param unifiedSymbol				合约编码
	 * @param openDir					开仓方向
	 * @param listenerType				监听类型
	 * @param basePrice					基准价格
	 * @param numOfPriceTickToTrigger	触发价差
	 * return 							监听对象
	 */
	IDisposablePriceListener priceTriggerOut(String unifiedSymbol, DirectionEnum openDir, DisposablePriceListenerType listenerType, double basePrice, int numOfPriceTickToTrigger, int volume);
	/**
	 * 止损止盈操作，达到价位会自动触发平仓逻辑
	 * 自动失效条件：
	 * 1. 触发一次后失效
	 * 2. 模组净持仓为零时
	 * 可通过 DisposablePriceListener.invalidate 方法主动失效
	 * 【注意】：止盈止损单没有实现状态持仓化，程序重启会导致其丢失，注意处理
	 * @param trade						成交对象
	 * @param listenerType				监听类型
	 * @param numOfPriceTickToTrigger	触发价差
	 * return 							监听对象
	 */
	IDisposablePriceListener priceTriggerOut(TradeField trade, DisposablePriceListenerType listenerType, int numOfPriceTickToTrigger);
	/**
	 * 获取模组周期设置
	 * @return
	 */
	int numOfMinPerMergedBar();
	/**
	 * 获取合约绑定的物理账户对象
	 * @param contract
	 * @return
	 */
	IAccount getAccount(ContractField contract);
	/**
	 * 
	 * @param contract
	 * @return
	 */
	IModuleAccount getModuleAccount(ContractField contract);
	/**
	 * 获取模组状态
	 * @return
	 */
	ModuleState getState();
	/**
	 * 停用模组策略
	 * @param enabled
	 */
	void disabledModule();
	/**
	 * 获取日志对象
	 * @return
	 */
	Logger getLogger();
	/**
	 * 创建指标
	 * @param configuration
	 * @param valueType
	 * @param indicatorFunction
	 * @return
	 */
	Indicator newIndicator(Indicator.Configuration configuration, ValueType valueType, TimeSeriesUnaryOperator indicatorFunction);
	/**
	 * 创建指标
	 * @param configuration
	 * @param indicatorFunction
	 * @return
	 */
	Indicator newIndicator(Indicator.Configuration configuration, TimeSeriesUnaryOperator indicatorFunction);
	/**
	 * 创建指标
	 * @param configuration
	 * @param indicatorFunction
	 * @return
	 */
	Indicator newIndicator(Indicator.Configuration configuration, Function<BarWrapper, TimeSeriesValue> indicatorFunction);
	/**
	 * 用指标方式透视值
	 * @param configuration
	 * @param value
	 */
	void viewValueAsIndicator(Indicator.Configuration configuration, AtomicDouble value);
	/**
	 * 绑定组合指标
	 * @param comboIndicator
	 */
	void addComboIndicator(IComboIndicator comboIndicator);
	/**
	 * 发消息提示
	 * @param content
	 */
	IMessageSender getMessageSender();
}
