package org.dromara.northstar.strategy;

import java.util.List;

import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.model.core.CancelOrderReq;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.indicator.Indicator;
import org.slf4j.Logger;

public interface IModuleStrategyContext {
	/**
	 * 委托下单（根据配置自动处理撤单追单）
	 * @param tradeIntent		交易意图
	 */
	void submitOrderReq(SubmitOrderReq orderReq);
	/**
	 * 撤单
	 * @param originOrderId		订单ID
	 */
	void cancelOrder(CancelOrderReq cancelReq);
	/**
	 * 获取合约绑定的物理账户对象
	 * @param contract
	 * @return
	 */
	IAccount getAccount(Contract contract);
	/**
	 * 获取模组账户
	 * @param contract
	 * @return
	 */
	IAccount getModuleAccount();
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
	 * 获取默认交易交易量（允许小数，如股票/加密货币的零碎股）
	 * @return
	 */
	double getDefaultVolume();
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
