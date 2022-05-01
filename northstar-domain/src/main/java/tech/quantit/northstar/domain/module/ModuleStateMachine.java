package tech.quantit.northstar.domain.module;

import tech.quantit.northstar.common.constant.ModuleState;
import tech.quantit.northstar.strategy.api.IModuleStateMachine;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TradeField;

public class ModuleStateMachine implements IModuleStateMachine {
	
	private ModuleState curState = ModuleState.EMPTY;
	
	private ModuleState prevState;

	@Override
	public void onOrder(OrderField order) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTrade(TradeField trade) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ModuleState getState() {
		return curState;
	}

	@Override
	public void onSubmitReq(SubmitOrderReqField orderReq) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCancelReq(CancelOrderReqField cancelReq) {
		// TODO Auto-generated method stub
		
	}

}
