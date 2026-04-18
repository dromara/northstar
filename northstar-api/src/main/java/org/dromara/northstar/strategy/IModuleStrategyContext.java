package org.dromara.northstar.strategy;

import java.util.List;
import java.util.Optional;

import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.constant.SignalOperation;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.strategy.constant.PriceType;
import org.dromara.northstar.strategy.model.TradeIntent;
import org.slf4j.Logger;

public interface IModuleStrategyContext {
	/**
	 * 委托下单（根据配置自动处理撤单追单）
	 * @param tradeIntent		交易意图
	 */
	void submitOrderReq(TradeIntent tradeIntent);
	/**
	 * 委托下单（精简接口）
	 * @param contract			交易合约			
	 * @param operation			操作信号
	 * @param priceType			价格类型
	 * @param volume			交易量
	 * @param price				委托价（市价为0）
	 * @return	originOrderId	订单ID
	 */
	Optional<String> submitOrderReq(Contract contract, SignalOperation operation, PriceType priceType, int volume, double price);
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
	 * 获取模组账户
	 * @param contract
	 * @return
	 */
	IModuleAccount getModuleAccount();
	/**
	 * 获取模组状态
	 * @return
	 */
	ModuleState getState();
	/**
	 * 获取合约
	 * @param unifiedSymbol		合约编码
	 * @return					返回合约信息
	 */
	Contract getContract(String unifiedSymbol);
	/**
	 * 获取模组绑定的全部合约
	 * @return
	 */
	List<Contract> bindedContracts();
	/**
	 * 获取运行状态
	 * @return
	 */
	boolean isEnabled();
	/**
	 * 获取日志对象
	 * @return
	 */
	Logger getLogger(Class<?> clz);
	/**
	 * 获取模组周期设置
	 * @return
	 */
	Interval moduleBarInterval();
	/**
	 * 获取默认交易交易量
	 * @return
	 */
	int getDefaultVolume();
	/**
	 * 停用模组策略
	 * @param enabled
	 */
	void disabledModule();
	/**
	 * 设置自定义的风控策略
	 * @param filter
	 */
	void setOrderRequestFilter(OrderRequestFilter filter);
	/**
	 * 注册指标
	 * @param indicator
	 */
	void registerIndicator(Indicator indicator);
	/**
	 * 注册周期监听器
	 * @param listener
	 */
	void addMergedBarListener(Contract contract, Interval interval, MergedBarListener listener);
}
