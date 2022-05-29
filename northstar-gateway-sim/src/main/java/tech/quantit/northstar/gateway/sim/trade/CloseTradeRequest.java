package tech.quantit.northstar.gateway.sim.trade;

import java.util.function.Consumer;

import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.utils.FieldUtils;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TradeField;


public class CloseTradeRequest extends TradeRequest {
	
	private TradePosition position;
	
	private SimAccount account;
	
	public CloseTradeRequest(SimAccount account, TradePosition position, FastEventEngine feEngine, Consumer<TradeRequest> doneCallback) {
		super(feEngine, doneCallback);
		this.account = account;
		this.position = position;
	}
	
	@Override
	protected OrderField initOrder(SubmitOrderReqField orderReq) {
		if(!FieldUtils.isClose(orderReq.getOffsetFlag())) {
			throw new IllegalArgumentException("传入非平仓请求");
		}
		return super.initOrder(orderReq);
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
		return position.totalAvailable() >= submitOrderReq.getVolume();
	}

	@Override
	public void onTrade(TradeField trade) {
		account.onCloseTrade(trade);
		account.reportAccountStatus();
	}

}
