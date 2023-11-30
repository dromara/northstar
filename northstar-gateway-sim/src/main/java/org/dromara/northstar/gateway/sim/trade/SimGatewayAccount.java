package org.dromara.northstar.gateway.sim.trade;

import org.dromara.northstar.common.model.SimAccountDescription;
import org.dromara.northstar.common.model.core.Account;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Trade;

import cn.hutool.core.lang.Assert;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.CurrencyEnum;

@Slf4j
public class SimGatewayAccount {

	private String gatewayId;
	@Getter
	private PositionManager positionManager;
	@Setter
	private OrderReqManager orderReqMgr;
	
	private double totalCloseProfit;
	
	private double totalCommission;
	
	private double totalDeposit;
	
	private double totalWithdraw;
	
	public SimGatewayAccount(String gatewayId) {
		this.gatewayId = gatewayId;
		this.positionManager = new PositionManager(this);
	}
	
	public SimGatewayAccount(SimAccountDescription accountDescription) {
		this.gatewayId = accountDescription.getGatewayId();
		this.positionManager = new PositionManager(this, accountDescription.getOpenTrades());
		this.totalCloseProfit = accountDescription.getTotalCloseProfit();
		this.totalCommission = accountDescription.getTotalCommission();
		this.totalDeposit = accountDescription.getTotalDeposit();
		this.totalWithdraw = accountDescription.getTotalWithdraw();
	}
	
	public Account account() {
		return Account.builder()
				.accountId(gatewayId)
				.available(available())
				.balance(balance())
				.closeProfit(totalCloseProfit)
				.commission(totalCommission)
				.gatewayId(gatewayId)
				.currency(CurrencyEnum.CNY)
				.name("模拟账户")
				.deposit(totalDeposit)
				.withdraw(totalWithdraw)
				.margin(orderReqMgr.totalFrozenAmount())
				.positionProfit(positionManager.totalHoldingProfit())
				.build();
	}
	
	public double balance() {
		// 当前权益 = 平仓盈亏 + 持仓盈亏 - 手续费 + 入金金额 - 出金金额
		return totalCloseProfit + positionManager.totalHoldingProfit() - totalCommission + totalDeposit - totalWithdraw;
	}
	
	public double available() {
		// 可用资金 = 当前权益 - 持仓保证金 - 委托单保证金
		return balance() - positionManager.totalMargin() - orderReqMgr.totalFrozenAmount();
	}
	
	public SimAccountDescription getAccountDescription() {
		return SimAccountDescription.builder()
				.gatewayId(gatewayId)
				.totalCloseProfit(totalCloseProfit)
				.totalCommission(totalCommission)
				.totalDeposit(totalDeposit)
				.totalWithdraw(totalWithdraw)
				.openTrades(positionManager.getNonclosedTrade())
				.build();
	}
	
	public void onTrade(Trade trade) {
		// 计算手续费
		Contract contract = trade.contract();
		double commission = contract.contractDefinition().getCommissionFee() > 0 
				? contract.contractDefinition().getCommissionFee() : contract.contractDefinition().getCommissionRate() * trade.price() * trade.contract().multiplier();
		double sumCommission = trade.volume() * commission;
		totalCommission += sumCommission;
		log.info("[{}] {} {} {} {}手 单边交易手续费：{}", gatewayId, trade.tradeDate(), trade.tradeTime(), trade.contract().name(), 
				trade.volume(), sumCommission);
		
		positionManager.onTrade(trade);
	}
	
	public void onDeal(Deal deal) {
		// 计算平仓盈亏
		totalCloseProfit += deal.profit();
		Trade trade = deal.getCloseTrade();
		log.info("[{}] {} {} {} {}手 平仓盈亏：{}", gatewayId, trade.tradeDate(), trade.tradeTime(), trade.contract().name(),
				trade.volume(), deal.profit());
		log.info("[{}] 累计平仓盈亏：{}，累计手续费：{}，当前持仓盈亏：{}，累计净出入金：{}，账户余额：{}", gatewayId, (int)totalCloseProfit, (int)totalCommission, 
				(int)positionManager.totalHoldingProfit(), (int)(totalDeposit - totalWithdraw), (int)balance());
	}
	
	public void onDeposit(double amount) {
		Assert.isTrue(amount >= 0, "不能为负数");
		totalDeposit += amount;
	}
	
	public void onWithdraw(double amount) {
		Assert.isTrue(amount >= 0, "不能为负数");
		totalWithdraw += amount;
	}
}
