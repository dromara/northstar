package tech.quantit.northstar.strategy.api;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;

import tech.quantit.northstar.common.constant.ModuleState;
import tech.quantit.northstar.common.constant.SignalOperation;
import tech.quantit.northstar.common.model.TimeSeriesValue;
import tech.quantit.northstar.strategy.api.constant.PriceType;
import tech.quantit.northstar.strategy.api.indicator.Indicator;
import tech.quantit.northstar.strategy.api.indicator.Indicator.ValueType;
import tech.quantit.northstar.strategy.api.indicator.TimeSeriesUnaryOperator;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;

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
	String submitOrderReq(ContractField contract, SignalOperation operation, PriceType priceType, int volume, double price);
	/**
	 * 止损止盈操作，达到价位会自动触发平仓逻辑
	 * 自动失效条件：
	 * 1. 触发一次后失效
	 * 2. 模组净持仓为零时
	 * 可通过 DisposablePriceListener.invalidate 方法主动失效
	 * 【注意】：止盈止损单没有实现状态持仓化，程序重启会导致其丢失，注意处理
	 * @param unifiedSymbol				合约编码
	 * @param openDir					开仓方向
	 * @param basePrice					基准价格
	 * @param numOfPriceTickToTrigger	触发价差（正数代表止盈，负数代表止损）
	 * return 							监听对象
	 */
	IDisposablePriceListener priceTriggerOut(String unifiedSymbol, DirectionEnum openDir, double basePrice, int numOfPriceTickToTrigger, int volume);
	/**
	 * 止损止盈操作，达到价位会自动触发平仓逻辑
	 * 自动失效条件：
	 * 1. 触发一次后失效
	 * 2. 模组净持仓为零时
	 * 可通过 DisposablePriceListener.invalidate 方法主动失效
	 * 【注意】：止盈止损单没有实现状态持仓化，程序重启会导致其丢失，注意处理
	 * @param contract					交易合约
	 * @param openDir					开仓方向
	 * @param basePrice					基准价格
	 * @param numOfPriceTickToTrigger	触发价差（正数代表止盈，负数代表止损）
	 * @param volume					手数
	 * return 							监听对象
	 */
	IDisposablePriceListener priceTriggerOut(ContractField contract, DirectionEnum openDir, double basePrice, int numOfPriceTickToTrigger, int volume);
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
	 * @param indicatorName			指标名称
	 * @param bindedUnifiedSymbol	绑定合约
	 * @param indicatorLength		指标缓存长度
	 * @param indicatorFunction		计算函数 
	 * @return
	 */
	Indicator newIndicator(String indicatorName, String bindedUnifiedSymbol, int indicatorLength,
			Function<BarField, TimeSeriesValue> indicatorFunction);
	/**
	 * 创建指标（采用默认长度16与收盘价取值）
	 * @param indicatorName			指标名称
	 * @param bindedUnifiedSymbol	绑定合约
	 * @param indicatorFunction		计算函数 
	 * @return
	 */
	Indicator newIndicator(String indicatorName, String bindedUnifiedSymbol, Function<BarField, TimeSeriesValue> indicatorFunction);
	/**
	 * 创建指标
	 * @param indicatorName			指标名称
	 * @param bindedUnifiedSymbol	绑定合约
	 * @param indicatorLength		指标缓存长度
	 * @param valTypeOfBar			取值类型
	 * @param indicatorFunction		计算函数
	 * @return
	 */
	Indicator newIndicator(String indicatorName, String bindedUnifiedSymbol, int indicatorLength, ValueType valTypeOfBar,
			TimeSeriesUnaryOperator indicatorFunction);
	/**
	 * 创建指标（采用默认长度16与收盘价取值）
	 * @param indicatorName			指标名称
	 * @param bindedUnifiedSymbol	绑定合约
	 * @param indicatorFunction		计算函数
	 * @return
	 */
	Indicator newIndicator(String indicatorName, String bindedUnifiedSymbol, TimeSeriesUnaryOperator indicatorFunction);
	/**
	 * 创建指标（采用默认收盘价取值）
	 * @param indicatorName			指标名称
	 * @param bindedUnifiedSymbol	绑定合约
	 * @param indicatorLength		指标缓存长度
	 * @param indicatorFunction		计算函数
	 * @return
	 */
	Indicator newIndicator(String indicatorName, String bindedUnifiedSymbol, int indicatorLength, TimeSeriesUnaryOperator indicatorFunction);
	/**
	 *  创建不同周期的指标
	 * @param numOfMinPerPeriod		周期分钟数
	 * @param indicatorName			指标名称
	 * @param bindedUnifiedSymbol	绑定合约
	 * @param indicatorLength		指标缓存长度
	 * @param indicatorFunction		计算函数
	 * @return
	 */
	Indicator newIndicatorAtPeriod(int numOfMinPerPeriod, String indicatorName, String bindedUnifiedSymbol, int indicatorLength, 
			Function<BarField, TimeSeriesValue> indicatorFunction);
	/**
	 * 创建不同周期的指标（采用默认长度16与收盘价取值）
	 * @param numOfMinPerPeriod		周期分钟数
	 * @param indicatorName			指标名称
	 * @param bindedUnifiedSymbol	绑定合约
	 * @param indicatorFunction		计算函数
	 * @return
	 */
	Indicator newIndicatorAtPeriod(int numOfMinPerPeriod, String indicatorName, String bindedUnifiedSymbol, 
			Function<BarField, TimeSeriesValue> indicatorFunction);
	/**
	 *  创建不同周期的指标
	 * @param numOfMinPerPeriod		周期分钟数
	 * @param indicatorName			指标名称
	 * @param bindedUnifiedSymbol	绑定合约
	 * @param indicatorLength		指标缓存长度
	 * @param valTypeOfBar			取值类型
	 * @param indicatorFunction		计算函数
	 * @return
	 */
	Indicator newIndicatorAtPeriod(int numOfMinPerPeriod, String indicatorName, String bindedUnifiedSymbol, int indicatorLength, 
			ValueType valTypeOfBar, TimeSeriesUnaryOperator indicatorFunction);
	/**
	 * 创建不同周期的指标（采用默认长度16与收盘价取值）
	 * @param numOfMinPerPeriod		周期分钟数
	 * @param indicatorName			指标名称
	 * @param bindedUnifiedSymbol	绑定合约
	 * @param indicatorFunction		计算函数
	 * @return
	 */
	Indicator newIndicatorAtPeriod(int numOfMinPerPeriod, String indicatorName, String bindedUnifiedSymbol, 
			TimeSeriesUnaryOperator indicatorFunction);
	/**
	 * 创建不同周期的指标（采用默认长度16与收盘价取值）
	 * @param numOfMinPerPeriod		周期分钟数
	 * @param indicatorName			指标名称
	 * @param bindedUnifiedSymbol	绑定合约
	 * @param indicatorLength		指标缓存长度
	 * @param indicatorFunction		计算函数
	 * @return
	 */
	Indicator newIndicatorAtPeriod(int numOfMinPerPeriod, String indicatorName, String bindedUnifiedSymbol, int indicatorLength,
			TimeSeriesUnaryOperator indicatorFunction);
	
	/**
	 * 获取日线数据
	 * @param contract
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	List<BarField> dailyBars(ContractField contract, LocalDate startDate, LocalDate endDate);
}
