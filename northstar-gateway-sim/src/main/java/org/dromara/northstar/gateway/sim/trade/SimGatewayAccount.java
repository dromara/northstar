package org.dromara.northstar.gateway.sim.trade;

import org.dromara.northstar.common.model.SimAccountDescription;

import com.google.protobuf.InvalidProtocolBufferException;

import cn.hutool.core.lang.Assert;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.CurrencyEnum;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TradeField;

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
		this.positionManager = new PositionManager(this, accountDescription.getOpenTrades().stream().map(this::convertTrade).toList());
		this.totalCloseProfit = accountDescription.getTotalCloseProfit();
		this.totalCommission = accountDescription.getTotalCommission();
		this.totalDeposit = accountDescription.getTotalDeposit();
		this.totalWithdraw = accountDescription.getTotalWithdraw();
	}
	
	private TradeField convertTrade(byte[] data) {
		try {
			return TradeField.parseFrom(data);
		} catch (InvalidProtocolBufferException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public AccountField accountField() {
		return AccountField.newBuilder()
				.setAccountId(gatewayId)
				.setAvailable(available())
				.setBalance(balance())
				.setCloseProfit(totalCloseProfit)
				.setCommission(totalCommission)
				.setGatewayId(gatewayId)
				.setCurrency(CurrencyEnum.CNY)
				.setName("模拟账户")
				.setDeposit(totalDeposit)
				.setWithdraw(totalWithdraw)
				.setMargin(orderReqMgr.totalFrozenAmount())
				.setPositionProfit(positionManager.totalHoldingProfit())
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
				.openTrades(positionManager.getNonclosedTrade().stream().map(TradeField::toByteArray).toList())
				.build();
	}
	
	public void onTrade(TradeField trade) {
		// 计算手续费
		ContractField contractField = trade.getContract();
		double commission = contractField.getCommissionFee() > 0 ? contractField.getCommissionFee() : contractField.getCommissionRate() * trade.getPrice() * trade.getContract().getMultiplier();
		double sumCommission = trade.getVolume() * commission;
		totalCommission += sumCommission;
		log.info("[{}] {} {} {} {}手 交易手续费：{}", gatewayId, trade.getTradeDate(), trade.getTradeTime(), trade.getContract().getName(), 
				trade.getVolume(), sumCommission);
		
		positionManager.onTrade(trade);
	}
	
	public void onDeal(Deal deal) {
		// 计算平仓盈亏
		totalCloseProfit += deal.profit();
		TradeField trade = deal.getCloseTrade();
		log.info("[{}] {} {} {} {}手 平仓盈亏：{}", gatewayId, trade.getTradeDate(), trade.getTradeTime(), trade.getContract().getName(),
				trade.getVolume(), deal.profit());
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
