package org.dromara.northstar.strategy;

import java.util.List;
import java.util.Optional;

import org.dromara.northstar.common.TransactionAware;
import org.dromara.northstar.common.exception.TradeException;
import org.dromara.northstar.common.model.core.CancelOrderReq;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Position;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.gateway.TradeGateway;

import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

public interface IAccount extends TransactionAware{
	
	/**
	 * 下单
	 * 所有的下单请求会先发账户发送，账户校验订单没问题后再转发去交易网关
	 * @param orderReq
	 * @throws TradeException	当委托单请求校验不通过时，会抛出这个异常
	 */
	void submitOrder(SubmitOrderReq orderReq) throws TradeException;
	/**
	 * 撤单
	 * 所有的撤单请求会先发账户发送，账户确保订单处于可撤单状态，才会转发去交易网关
	 * @param cancelReq	撤单请求
	 * @return	
	 */
	boolean cancelOrder(CancelOrderReq cancelReq);
	/**
	 * 账户权益
	 * @return
	 */
	double accountBalance();
	/**
	 * 实际可用余额
	 * @return
	 */
	double availableBalance();
	/**
	 * 账户ID
	 * @return
	 */
	String getId();
	/**
	 * 获取某合约的持仓信息
	 * @param posDirection
	 * @param unifiedSymbol
	 * @return
	 */
	Optional<Position> getPosition(PositionDirectionEnum posDirection, Contract contract);
	/**
	 * 获取未完成订单
	 * @return
	 */
	List<Order> getPendingOrders();
	/**
	 * 获取账户绑定的交易网关
	 * @return
	 */
	TradeGateway getTradeGateway();
}
