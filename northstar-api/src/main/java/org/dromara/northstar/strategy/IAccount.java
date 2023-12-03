package org.dromara.northstar.strategy;

import java.util.Optional;
import java.util.UUID;

import org.dromara.northstar.common.TransactionAware;
import org.dromara.northstar.common.model.core.Account;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Position;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.gateway.MarketGateway;
import org.dromara.northstar.gateway.TradeGateway;

import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

public interface IAccount extends TransactionAware{
	
	/**
	 * 下单
	 * @param orderReq	委托请求
	 * @return			订单ID（originOrderId）
	 */
	String submitOrder(SubmitOrderReq orderReq);
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
	void onAccount(Account account);
	/**
	 * 响应持仓事件
	 * @param position
	 */
	void onPosition(Position position);
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
	int netPosition(Contract contract);
	/**
	 * 获取某合约的持仓信息
	 * @param posDirection
	 * @param unifiedSymbol
	 * @return
	 */
	Optional<Position> getPosition(PositionDirectionEnum posDirection, Contract contract);
	/**
	 * 获取账户绑定的行情网关
	 * @return
	 */
	MarketGateway getMarketGateway();
	/**
	 * 获取账户绑定的交易网关
	 * @return
	 */
	TradeGateway getTradeGateway();
}
