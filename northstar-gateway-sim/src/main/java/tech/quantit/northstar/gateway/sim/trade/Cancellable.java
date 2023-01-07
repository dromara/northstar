package tech.quantit.northstar.gateway.sim.trade;

import com.google.common.eventbus.Subscribe;

import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.OrderField;

public interface Cancellable {

	@Subscribe
	OrderField onCancal(CancelOrderReqField cancelReq);
}
