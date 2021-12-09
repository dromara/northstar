package tech.quantit.northstar.gateway.sim.trade;

import java.util.function.Consumer;

import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.utils.FieldUtils;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;


public class CloseTradeRequest extends TradeRequest {
	
	private SimPosition position;
	
	public CloseTradeRequest(SimPosition position, FastEventEngine feEngine, SubmitOrderReqField submitOrderReq, Consumer<TradeRequest> doneCallback) {
		super(feEngine, submitOrderReq, doneCallback);
		if(!FieldUtils.isClose(submitOrderReq.getOffsetFlag())) {
			throw new IllegalArgumentException("传入非平仓请求");
		}
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
		
}
