package org.dromara.northstar.strategy;

import org.dromara.northstar.common.TransactionAware;
import org.dromara.northstar.common.constant.ModuleState;

import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;

public interface IModuleStateMachine extends TransactionAware{

	ModuleState getState();
	
	void onSubmitReq(SubmitOrderReqField orderReq);
	
	void onCancelReq(CancelOrderReqField cancelReq);
}
