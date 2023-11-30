package org.dromara.northstar.indicator;

import java.util.Objects;
import java.util.Optional;

import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Tick;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 即时K线生成器
 * 根据TICK数据生成即时K线 
 * @author KevinHuangwl
 *
 */
public class InstantBarGenerator {

	private BarField.Builder bb;
	
	private ContractField contract;
	
	public InstantBarGenerator(ContractField contract) {
		this.contract = contract;
	}

	public synchronized Optional<Bar> update(Tick tick) {
		if (!contract.getUnifiedSymbol().equals(tick.getUnifiedSymbol())) {
			throw new IllegalArgumentException("合约不匹配，期望合约：" + contract.getUnifiedSymbol() + "，实际合约：" + tick.getUnifiedSymbol());
		}

		if(Objects.nonNull(bb) && tick.getActionTimestamp() % 60000 == 0) {
			bb = null;
			return Optional.empty();
		}
		
		if(bb == null) {
			bb = BarField.newBuilder();
			bb.setActionDay(tick.getActionDay());
			bb.setActionTime(tick.getActionTime());
			bb.setActionTimestamp(tick.getActionTimestamp());
			bb.setTradingDay(tick.getTradingDay());
			bb.setOpenPrice(tick.getLastPrice());
			bb.setHighPrice(tick.getLastPrice());
			bb.setLowPrice(tick.getLastPrice());
			bb.setClosePrice(tick.getLastPrice());
			bb.setUnifiedSymbol(tick.getUnifiedSymbol());
			bb.setGatewayId(tick.getGatewayId());
		}
		
		bb.setHighPrice(Math.max(bb.getHighPrice(), tick.getLastPrice()));
		bb.setLowPrice(Math.min(bb.getLowPrice(), tick.getLastPrice()));
		bb.setClosePrice(tick.getLastPrice());
		bb.setOpenInterest(tick.getOpenInterest());
		bb.setVolume(bb.getVolume() + tick.getVolumeDelta());
		bb.setTurnover(tick.getTurnover());
		bb.setNumTrades(tick.getNumTrades());
		bb.setOpenInterestDelta(bb.getOpenInterestDelta() + tick.getOpenInterestDelta());
		bb.setTurnoverDelta(bb.getTurnoverDelta() + tick.getTurnoverDelta());
		bb.setNumTradesDelta(bb.getNumTradesDelta() + tick.getNumTradesDelta());
		return Optional.of(bb.build());
	}
	
}
