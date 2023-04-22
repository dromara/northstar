package org.dromara.northstar.module;

import java.util.List;

import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.data.IModuleRepository;
import org.dromara.northstar.strategy.IModuleAccount;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

public class ModuleAccount implements IModuleAccount{

	private ModuleDescription moduleDescription;
	private ModuleRuntimeDescription moduleRtDescription;
	private IModuleRepository moduleRepo;
	
	public ModuleAccount(ModuleDescription moduleDescription, ModuleRuntimeDescription moduleRtDescription, IModuleRepository moduleRepo) {
		this.moduleDescription = moduleDescription;
		this.moduleRtDescription = moduleRtDescription;
		this.moduleRepo = moduleRepo;
	}

	@Override
	public void onTick(TickField tick) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onOrder(OrderField order) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTrade(TradeField trade) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public double getInitBalance(String gatewayId) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getPreBalance(String gatewayId) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getAccCommission(String gatewayId) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<PositionField> getPositions(String gatewayId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<TradeField> getNonclosedTrades(String gatewayId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<TradeField> getNonclosedTrades(String unifiedSymbol, DirectionEnum direction) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getNonclosedPosition(String unifiedSymbol, DirectionEnum direction) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNonclosedNetPosition(String unifiedSymbol) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getAccDealVolume(String gatewayId) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getAccCloseProfit(String gatewayId) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getMaxDrawBack(String gatewayId) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getMaxProfit(String gatewayId) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void tradeDayPreset() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ModuleState getModuleState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onSubmitOrder(SubmitOrderReqField submitOrder) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCancelOrder(CancelOrderReqField cancelOrder) {
		// TODO Auto-generated method stub
		
	}
	
	
}
