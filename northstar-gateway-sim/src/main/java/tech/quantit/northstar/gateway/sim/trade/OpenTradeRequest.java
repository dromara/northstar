package tech.quantit.northstar.gateway.sim.trade;

import java.util.function.Consumer;

import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.utils.FieldUtils;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TradeField;

public class OpenTradeRequest extends TradeRequest {
	
	private SimAccount account;
	
	public OpenTradeRequest(SimAccount account, FastEventEngine feEngine, SubmitOrderReqField submitOrderReq, Consumer<TradeRequest> doneCallback) {
		super(feEngine, submitOrderReq, doneCallback);
		if(!FieldUtils.isOpen(submitOrderReq.getOffsetFlag())) {
			throw new IllegalArgumentException("传入非开仓请求");
		}
		this.account = account;
		this.initOrder(submitOrderReq);
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
	public void onTrade(TradeField trade) {
		account.addPosition(new SimPosition(trade), trade);
		account.addCommission(trade.getVolume());
	}
	
}
