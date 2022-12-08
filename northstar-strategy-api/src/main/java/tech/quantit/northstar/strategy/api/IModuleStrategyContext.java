package tech.quantit.northstar.strategy.api;

import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;

import com.google.common.util.concurrent.AtomicDouble;

import tech.quantit.northstar.common.constant.ModuleState;
import tech.quantit.northstar.common.constant.SignalOperation;
import tech.quantit.northstar.common.model.BarWrapper;
import tech.quantit.northstar.common.model.TimeSeriesValue;
import tech.quantit.northstar.strategy.api.constant.DisposablePriceListenerType;
import tech.quantit.northstar.strategy.api.constant.PriceType;
import tech.quantit.northstar.strategy.api.indicator.Indicator;
import tech.quantit.northstar.strategy.api.indicator.Indicator.ValueType;
import tech.quantit.northstar.strategy.api.indicator.TimeSeriesUnaryOperator;
import tech.quantit.northstar.strategy.api.utils.trade.TradeIntent;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TradeField;

public interface IModuleStrategyContext {
	/**
	 * 获取模组名称
	 * @return
	 */
	String getModuleName();
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
	 * 委托下单（精简接口）
	 * @param contract			交易合约			
	 * @param operation			操作信号
	 * @param priceType			价格类型
	 * @param volume			手数
	 * @param price				委托价（市价为0）
	 * @return	originOrderId	订单ID
	 */
	Optional<String> submitOrderReq(ContractField contract, SignalOperation operation, PriceType priceType, int volume, double price);
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
	 * 判断订单是否已经超时
	 * 该方法用于撤单场景
	 * @param originOrderId		订单ID
	 * @param timeout			超时毫秒数
	 * @return
	 */
	boolean isOrderWaitTimeout(String originOrderId, long timeout);
	/**
	 * 撤单
	 * @param originOrderId		订单ID
	 */
	void cancelOrder(String originOrderId);
	/**
	 * 获取模组周期设置
	 * @return
	 */
	int numOfMinPerModuleBar();
	/**
	 * 模组持仓净盈亏
	 * @return
	 */
	int holdingNetProfit();
	/**
	 * 模组可用持仓（全部）
	 * @param direction			持仓方向
	 * @param unifiedSymbol 	合约编码
	 * @return
	 */
	int availablePosition(DirectionEnum direction, String unifiedSymbol);
	/**
	 * 模组可用持仓（明细）
	 * @param direction			持仓方向
	 * @param unifiedSymbol		合约编码
	 * @param isToday			是否为当天持仓
	 * @return
	 */
	int availablePosition(DirectionEnum direction, String unifiedSymbol, boolean isToday);
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
}
