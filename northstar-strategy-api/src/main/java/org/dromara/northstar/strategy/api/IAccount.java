package org.dromara.northstar.strategy.api;

import java.util.Optional;
import java.util.UUID;

import org.dromara.northstar.common.TransactionAware;

import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;

public interface IAccount extends TransactionAware{
	
	/**
	 * 下单
	 * @param orderReq	委托请求
	 * @return			订单ID（originOrderId）
	 */
	String submitOrder(SubmitOrderReqField orderReq);
	/**
	 * 撤单
	 * @param originOrderId	订单ID
	 * @return
	 */
	boolean cancelOrder(String originOrderId);
	/**
	 * 账户权益
	 * @return
	 */
	double accountBalance();
	/**
	 * 可用余额
	 * @return
	 */
	double availableAmount();
	/**
	 * 账户风险度
	 * 风险度取值范围为0到1之间，风险度越高，被强平的风险越大
	 * @return	[0, 1)
	 */
	double degreeOfRisk();
	/**
	 * 锁定账户金额
	 * @return
	 */
	Optional<UUID> tryLockAmount(double amount);
	
	
	
	/**
	 * 响应账户事件
	 * @param account
	 */
	void onAccount(AccountField account);
	/**
	 * 响应持仓事件
	 * @param position
	 */
	void onPosition(PositionField position);
	
}
