package org.dromara.northstar.strategy.api;

import java.util.Optional;
import java.util.UUID;

import org.dromara.northstar.common.TransactionAware;
import org.dromara.northstar.gateway.api.MarketGateway;
import org.dromara.northstar.gateway.api.TradeGateway;

import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
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
	boolean cancelOrder(CancelOrderReqField cancelReq);
	/**
	 * 账户权益
	 * @return
	 */
	double accountBalance();
	/**
	 * 实际可用余额
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
	 * 适用于多个模组同时抢占金额，使账户金额利用率最大化
	 * @return	lockID
	 */
	Optional<UUID> tryLockAmount(double amount);
	/**
	 * 解锁账户金额
	 * @param lockId
	 */
	void unlockAmount(UUID lockId);
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
	/**
	 * 账户ID
	 * @return
	 */
	String accountId();
	/**
	 * 获取某合约的净持仓
	 * @param unifiedSymbol	合约代码
	 * @return				净持仓：正数代表净多头，负数代表净空头
	 */
	int netPosition(String unifiedSymbol);
	
	
	MarketGateway getMarketGateway();
	
	TradeGateway getTradeGateway();
}
