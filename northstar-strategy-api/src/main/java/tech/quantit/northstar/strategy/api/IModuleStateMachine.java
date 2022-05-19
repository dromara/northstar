package tech.quantit.northstar.strategy.api;

import tech.quantit.northstar.common.TransactionAware;
import tech.quantit.northstar.common.constant.ModuleState;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;

public interface IModuleStateMachine extends TransactionAware{

	ModuleState getState();
	
	void onSubmitReq(SubmitOrderReqField orderReq);
	
	void onCancelReq(CancelOrderReqField cancelReq);
}
