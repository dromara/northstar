package org.dromara.northstar.gateway.sim.trade;

import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.OrderField;

public interface Cancellable {

	
	OrderField onCancal(CancelOrderReqField cancelReq);
}
