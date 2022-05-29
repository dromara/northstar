package tech.quantit.northstar.gateway.sim.trade;

import com.google.common.eventbus.Subscribe;

import tech.quantit.northstar.common.Subscribable;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.OrderField;

public interface Cancellable extends Subscribable{

	@Subscribe
	OrderField onCancal(CancelOrderReqField cancelReq);
}
