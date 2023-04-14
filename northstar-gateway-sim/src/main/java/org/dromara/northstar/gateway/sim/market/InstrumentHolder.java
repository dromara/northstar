package org.dromara.northstar.gateway.sim.market;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.dromara.northstar.common.constant.DateTimeConstant;

import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;

public class InstrumentHolder {
	
	private double seed = Math.random();
	
	private ContractField contract;
	
	private TickField.Builder tb = TickField.newBuilder();
	
	public InstrumentHolder(ContractField contract) {
		this.contract = contract;
		LocalDateTime ldt = LocalDateTime.now();
		tb
		.setLastPrice(InstrumentBasePrice.getBasePrice(contract))
		.setSettlePrice(InstrumentBasePrice.getBasePrice(contract))
		.setPreSettlePrice(InstrumentBasePrice.getBasePrice(contract))
		.setGatewayId(contract.getGatewayId())
		.addAllAskPrice(List.of(0D, 0D, 0D, 0D, 0D))
		.addAllBidPrice(List.of(0D, 0D, 0D, 0D, 0D))
		.addAllAskVolume(List.of(0, 0, 0, 0, 0))
		.addAllBidVolume(List.of(0, 0, 0, 0, 0))
		.setUnifiedSymbol(contract.getUnifiedSymbol())
		.setActionDay(ldt.format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
		.setActionTime(ldt.format(DateTimeConstant.T_FORMAT_WITH_MS_INT_FORMATTER))
		.setActionTimestamp(ldt.toInstant(ZoneOffset.ofHours(8)).toEpochMilli());
	}
	
	public TickField.Builder getLastTick(){
		return tb;
	}
	
	public void setLastTick(TickField.Builder tb) {
		this.tb = tb;
	}
	
	public ContractField getContract() {
		return contract;
	}
	
	public double getSeed() {
		return seed;
	}
	
	public void setSeed(double seed) {
		this.seed = seed;
	}
	
}
