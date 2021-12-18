package tech.quantit.northstar.gateway.sim.trade;

import com.google.common.eventbus.Subscribe;

import tech.quantit.northstar.common.Subscribable;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;

public interface Cancellable extends Subscribable{

	@Subscribe
	void onCancal(CancelOrderReqField cancelReq);
}
