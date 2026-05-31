package org.dromara.northstar.module;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.dromara.northstar.common.ModuleLoggerAware;
import org.dromara.northstar.common.exception.IllegalOrderRequestException;
import org.dromara.northstar.common.exception.TradeException;
import org.dromara.northstar.common.model.core.CancelOrderReq;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Position;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.common.utils.FieldUtils;
import org.dromara.northstar.gateway.TradeGateway;
import org.dromara.northstar.gateway.sim.trade.AccountPosition;
import org.dromara.northstar.strategy.IAccount;
import org.slf4j.Logger;

import com.google.common.collect.Table;

import lombok.Getter;
import lombok.Setter;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

/**
 * 模组账户
 * 一个模组账户可以对应多个真实账户信息
 * 仅统计与模组相关的账户状态与持仓状态；
 * 同时负责下单请求的路由分发，将订单转发到对应的交易账户
 * @author KevinHuangwl
 *
 */
public class ModuleAccount implements IAccount, ModuleLoggerAware {

	@Setter
	private Logger logger;

	private final String moduleName;
	private final double initBalance;
	private final double initCommission;
	private final double initRealizedProfit;
	
	/* 合约 -> 真实交易账户 路由映射 */
	@Getter
	private final Map<Contract, IAccount> contractAccountMap;
	private final Table<Contract, PositionDirectionEnum, AccountPosition> positionTbl;

	public ModuleAccount(String moduleName, double initBalance, double initRealizedProfit, double initCommission,
			Table<Contract, PositionDirectionEnum, AccountPosition> positionTbl, Map<Contract, IAccount> contractAccountMap) {
		this.moduleName = moduleName;
		this.initBalance = initBalance;
		this.initCommission = initCommission;
		this.initRealizedProfit = initRealizedProfit;
		this.contractAccountMap = contractAccountMap;
		this.positionTbl = positionTbl;
	}
	
	@Override
	public void submitOrder(SubmitOrderReq orderReq) throws TradeException {
		IAccount tradeAccount = contractAccountMap.get(orderReq.contract());
		if(tradeAccount == null) {
			throw new IllegalStateException("找不到对应的交易账户");
		}
		if(FieldUtils.isOpen(orderReq.offsetFlag())) {
			checkIfHasSufficientAmount(orderReq);
		} else {
			checkIfHasSufficientPosition(orderReq);
		}
		
		tradeAccount.submitOrder(orderReq);
	}

	
	private void checkIfHasSufficientAmount(SubmitOrderReq submitOrder) {
		double margin = submitOrder.contract().contractDefinition().marginRatio();
		double cost = submitOrder.price() * submitOrder.volume() * submitOrder.contract().multiplier() * margin;
		if(availableBalance() < cost) {
			logger.warn("[{}] 资金不足。可用资金为：{}，开仓成本为：{}", getId(), availableBalance(), cost);
			throw new IllegalOrderRequestException();
		}
	}
	
	private void checkIfHasSufficientPosition(SubmitOrderReq submitOrder) {
		Contract contract = submitOrder.contract();
		PositionDirectionEnum closeDir = FieldUtils.closePositionDirOf(submitOrder.direction());
		AccountPosition position = positionTbl.get(contract, closeDir);
		if(position == null) {
			throw new IllegalStateException("找不到持仓对象");
		}
		double availablePos = position.position() - position.frozen();
		if(availablePos < submitOrder.volume()) {
			logger.warn("[{}] 持仓不够。可用持仓为：{}，委托交易量：{}", getId(), availablePos, submitOrder.volume());
			throw new IllegalOrderRequestException();
		}
	}
	
	@Override
	public boolean cancelOrder(CancelOrderReq cancelReq) {
		List<IAccount> accounts = contractAccountMap.values().stream().filter(acc -> acc.getId().equals(cancelReq.gatewayId())).toList();
		if(accounts.isEmpty()) {
			throw new IllegalStateException("没有找到对应的账户：" + cancelReq.gatewayId());
		}
		return accounts.getFirst().cancelOrder(cancelReq);
	}

	@Override
	public double accountBalance() {
		return initBalance + realizedProfit() - commission();
	}
	
	// 持仓占用
	private double holdingMargin() {
		return positionTbl.values().stream().mapToDouble(AccountPosition::margin).sum();
	}

	public double realizedProfit() {
		return initRealizedProfit + positionTbl.values().stream().mapToDouble(AccountPosition::getAccRealizedProfit).sum();
	}
	
	public double commission() {
		return initCommission + positionTbl.values().stream().mapToDouble(AccountPosition::getAccCommission).sum();
	}

	@Override
	public double availableBalance() {
		return accountBalance() - holdingMargin();
	}

	@Override
	public String getId() {
		return moduleName;
	}

	@Override
	public Optional<Position> getPosition(PositionDirectionEnum posDirection, Contract contract) {
		AccountPosition mp = positionTbl.get(contract, posDirection);
		if(mp == null) {
			return Optional.empty();
		}
		return Optional.of(mp.convertToPosition());
	}
	
	public List<Position> getPositions(){
		return positionTbl.values().stream().map(AccountPosition::convertToPosition).toList();
	}
	
	public List<AccountPosition> getModulePositions(){
		return positionTbl.values().stream().toList();
	}

	@Override
	public List<Order> getPendingOrders() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void onOrder(Order order) {
		// 不需要处理
	}

	@Override
	public void onTrade(Trade trade) {
		positionTbl.values().forEach(ap -> ap.onTrade(trade));
	}
	
	public void onTick(Tick tick) {
		positionTbl.values().forEach(ap -> ap.onTick(tick));
	}

	@Override
	public TradeGateway getTradeGateway() {
		throw new UnsupportedOperationException();
	}
	
}
