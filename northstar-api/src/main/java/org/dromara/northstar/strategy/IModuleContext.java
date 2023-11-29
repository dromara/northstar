package org.dromara.northstar.strategy;

import java.util.List;
import java.util.Optional;

import org.dromara.northstar.common.BarDataAware;
import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.common.TransactionAware;
import org.dromara.northstar.common.constant.SignalOperation;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.strategy.constant.PriceType;

import xyz.redtorch.pb.CoreField.BarField;

public interface IModuleContext extends IModuleStrategyContext, MergedBarListener, TickDataAware, BarDataAware, TransactionAware {
	/**
	 * 预热模组
	 * @param barData
	 */
	void initData(List<BarField> barData);
	/**
	 * 获取模组状态
	 * @return
	 */
	ModuleRuntimeDescription getRuntimeDescription(boolean fullDescription);
	/**
	 * 设置模组
	 * @param module
	 */
	void setModule(IModule module);
	/**
	 * 获取模组
	 * @return
	 */
	IModule getModule();
	/**
	 * 委托下单（精简接口）
	 * @param contract			交易合约			
	 * @param operation			操作信号
	 * @param priceType			价格类型
	 * @param volume			手数
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
	 * 设置运行状态
	 * @param enabled
	 */
	void setEnabled(boolean enabled);
	/**
	 * 获取运行状态
	 * @return
	 */
	boolean isEnabled();
	/**
	 * 是否已完成初始化
	 * @return
	 */
	boolean isReady();
	/**
	 * 完成初始化
	 */
	void onReady();
}
