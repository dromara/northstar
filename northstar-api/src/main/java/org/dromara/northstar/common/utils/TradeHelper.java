package org.dromara.northstar.common.utils;

import java.util.Optional;

import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Position;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.strategy.IAccount;
import org.dromara.northstar.strategy.IModuleStrategyContext;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

public class TradeHelper {

	private Contract contract;
	private IModuleStrategyContext ctx;
	
	public TradeHelper(IModuleStrategyContext ctx, Contract contract) {
		this.contract = contract;
		this.ctx = ctx;
	}
	
	public void buyOpen(double volume) {
		submitOrder(DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open, 0, volume);
	}
	
	public void sellOpen(double volume) {
		submitOrder(DirectionEnum.D_Sell, OffsetFlagEnum.OF_Open, 0, volume);
	}
	
	public void buyClose(double volume) {
		submitOrder(DirectionEnum.D_Buy, OffsetFlagEnum.OF_Close, 0, volume);
	}
	
	public void sellClose(double volume) {
		submitOrder(DirectionEnum.D_Sell, OffsetFlagEnum.OF_Close, 0, volume);
	}
	
	public void buyOpen(double price, double volume) {
		submitOrder(DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open, price, volume);
	}
	
	public void sellOpen(double price, double volume) {
		submitOrder(DirectionEnum.D_Sell, OffsetFlagEnum.OF_Open, price, volume);
	}
	
	public void buyClose(double price, double volume) {
		submitOrder(DirectionEnum.D_Buy, OffsetFlagEnum.OF_Close, price, volume);
	}
	
	public void sellClose(double price, double volume) {
		submitOrder(DirectionEnum.D_Sell, OffsetFlagEnum.OF_Close, price, volume);
	}
	
	public void buyCloseAll(double price) {
		Optional<Position> position = ctx.getModuleAccount().getPosition(PositionDirectionEnum.PD_Short, contract);
		if(position.isEmpty()) {
			ctx.getLogger(getClass()).warn("找不到对应的空头持仓：{}", contract.name());
		} else {
			buyClose(price, position.get().position());
		}
	}
	
	public void buyCloseAll() {
		buyCloseAll(0);
	}
	
	public void sellCloseAll(double price) {
		Optional<Position> position = ctx.getModuleAccount().getPosition(PositionDirectionEnum.PD_Long, contract);
		if(position.isEmpty()) {
			ctx.getLogger(getClass()).warn("找不到对应的多头持仓：{}", contract.name());
		} else {
			sellClose(price, position.get().position());
		}
	}
	
	public void sellCloseAll() {
		sellCloseAll(0);
	}
	
	private void submitOrder(DirectionEnum dir, OffsetFlagEnum flag, double price, double volume) {
		IAccount account = ctx.getAccount(contract);
		ctx.submitOrderReq(SubmitOrderReq.builder()
				.gatewayId(account.getId())
				.contract(contract)
				.direction(dir)
				.offsetFlag(flag)
				.price(price)
				.volume(volume)
				.build());
	}
}
