package org.dromara.northstar.strategy.api;

import java.util.List;

import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.common.TransactionAware;
import org.dromara.northstar.common.constant.ModuleState;

import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 
 * @author KevinHuangwl
 *
 */
public interface IModuleAccountStore extends TickDataAware, TransactionAware {
	/**
	 * 初始余额
	 * @param gatewayId		账户ID
	 * @return
	 */
	double getInitBalance(String gatewayId);
	/**
	 * 期初余额
	 * @param gatewayId		账户ID
	 * @return
	 */
	double getPreBalance(String gatewayId);
	/**
	 * 累计交易手续费
	 * @param gatewayId		账户ID
	 * @return
	 */
	double getAccCommission(String gatewayId);
	/**
	 * 获取持仓信息
	 * @param gatewayId		账户ID
	 * @return
	 */
	List<PositionField> getPositions(String gatewayId);
	/**
	 * 获取全部未平仓成交
	 * @param gatewayId		账户ID
	 * @return
	 */
	List<TradeField> getUncloseTrades(String gatewayId);
	/**
	 * 获取累计开平仓手数
	 * @param gatewayId		账户ID
	 * @return
	 */
	int getAccDealVolume(String gatewayId);
	/**
	 * 获取累计平仓盈亏
	 * @param gatewayId		账户ID
	 * @return
	 */
	double getAccCloseProfit(String gatewayId);
	/**
	 * 获取最大回撤
	 * @param gatewayId		账户ID
	 * @return
	 */
	double getMaxDrawBack(String gatewayId);
	/**
	 * 获取最大利润
	 * @param gatewayId
	 * @return
	 */
	double getMaxProfit(String gatewayId);
	/**
	 * 新交易日开盘前处理
	 */
	void tradeDayPreset();
	/**
	 * 获取模组状态
	 * @return
	 */
	ModuleState getModuleState();
	/**
	 * 响应下单
	 * @param submitOrder
	 */
	void onSubmitOrder(SubmitOrderReqField submitOrder);
	/**
	 * 响应撤单
	 * @param cancelOrder
	 */
	void onCancelOrder(CancelOrderReqField cancelOrder);
}
