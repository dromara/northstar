package tech.quantit.northstar.gateway.sim.trade;

import java.util.function.Consumer;

import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.utils.FieldUtils;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TradeField;


public class CloseTradeRequest extends TradeRequest {
	
	private SimPosition position;
	
	private SimAccount account;
	
	public CloseTradeRequest(SimAccount account, SimPosition position, FastEventEngine feEngine, SubmitOrderReqField submitOrderReq, Consumer<TradeRequest> doneCallback) {
		super(feEngine, submitOrderReq, doneCallback);
		if(!FieldUtils.isClose(submitOrderReq.getOffsetFlag())) {
			throw new IllegalArgumentException("传入非平仓请求");
		}
		this.account = account;
		this.position = position;
		this.initOrder(submitOrderReq);
	}

	/**
	 * 
	 * @return
	 */
	public int frozenVol() {
		return submitOrderReq.getVolume();
	}

	@Override
	protected boolean canMakeOrder() {
		return position.availableVol() >= submitOrderReq.getVolume();
	}

	@Override
	public void onTrade(TradeField trade) {
		int factor = FieldUtils.isLong(position.getDirection()) ? 1 : -1;
		account.addCloseProfit(factor * frozenVol() * (trade.getPrice() - position.getOpenPrice()) * position.getMultipler());
		account.updateCommission(trade);
		position.setCloseReq(null);
		position.merge(trade);
		account.reportAccountStatus();
	}

	@Override
	public void onCancal(CancelOrderReqField cancelReq) {
		super.onCancal(cancelReq);
		position.setCloseReq(null);
	}
	
	
		
}
