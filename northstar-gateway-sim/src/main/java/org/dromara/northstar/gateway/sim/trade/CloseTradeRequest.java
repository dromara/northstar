package org.dromara.northstar.gateway.sim.trade;

import java.util.function.Consumer;

import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.utils.FieldUtils;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TradeField;

@Slf4j
public class CloseTradeRequest extends TradeRequest {
	
	private TradePosition position;
	
	private SimAccount account;
	
	public CloseTradeRequest(SimAccount account, TradePosition position, FastEventEngine feEngine, Consumer<TradeRequest> doneCallback) {
		super(feEngine, doneCallback);
		this.account = account;
		this.position = position;
	}
	
	@Override
	protected synchronized OrderField initOrder(SubmitOrderReqField orderReq) {
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
		boolean valid = position.totalAvailable() >= submitOrderReq.getVolume();
		if(!valid) {
			log.warn("[{}] 持仓不足。可用持仓：{}，平仓委托数：{}", account.gatewayId(), position.totalAvailable(), submitOrderReq.getVolume());
		}
		return valid;
	}

	@Override
	public synchronized void onTrade(TradeField trade) {
		account.onCloseTrade(trade);
		account.reportAccountStatus();
	}

}
