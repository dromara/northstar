package org.dromara.northstar.gateway.sim.trade;

import java.util.function.Consumer;

import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.utils.FieldUtils;

import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TradeField;

public class OpenTradeRequest extends TradeRequest {
	
	private SimAccount account;
	
	public OpenTradeRequest(SimAccount account, FastEventEngine feEngine, Consumer<TradeRequest> doneCallback) {
		super(feEngine, doneCallback);
		this.account = account;
	}
	
	@Override
	protected synchronized OrderField initOrder(SubmitOrderReqField orderReq) {
		if(!FieldUtils.isOpen(orderReq.getOffsetFlag())) {
			throw new IllegalArgumentException("传入非开仓请求");
		}
		return super.initOrder(orderReq);
	}

	public double frozenAmount() {
		ContractField contract = submitOrderReq.getContract();
		int vol = submitOrderReq.getVolume();
		double multipler = contract.getMultiplier();
		double price = submitOrderReq.getPrice();
		double marginRatio = FieldUtils.isBuy(submitOrderReq.getDirection()) ? contract.getLongMarginRatio() : contract.getShortMarginRatio();
		return price * vol * multipler * marginRatio;
	}

	@Override
	protected boolean canMakeOrder() {
		return frozenAmount() < this.account.available();
	}

	@Override
	public synchronized void onTrade(TradeField trade) {
		account.onOpenTrade(trade);
		account.reportAccountStatus();
	}
	
}
