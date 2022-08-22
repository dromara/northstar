package tech.quantit.northstar.domain.module;

import tech.quantit.northstar.common.constant.ClosingPolicy;
import tech.quantit.northstar.common.constant.SignalOperation;
import tech.quantit.northstar.strategy.api.ClosingStrategy;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.PositionField;

/**
 * 先开先平策略
 * @author KevinHuangwl
 *
 */
public class FirstInFirstOutClosingStrategy implements ClosingStrategy{

	@Override
	public OffsetFlagEnum resolveOperation(SignalOperation opr, PositionField position) {
		if(opr.isOpen())	return OffsetFlagEnum.OF_Open;
		if(position.getPosition() - position.getFrozen() < 1) {
			throw new IllegalStateException("没有足够持仓可用");
		}
		ContractField contract = position.getContract();
		if(contract.getExchange() == ExchangeEnum.SHFE) {			
			if(position.getYdPosition() - position.getYdFrozen() > 0)	return OffsetFlagEnum.OF_Close;
			if(position.getTdPosition() - position.getTdFrozen() > 0)	return OffsetFlagEnum.OF_CloseToday;
		}
		return OffsetFlagEnum.OF_Close;
	}

	@Override
	public ClosingPolicy getClosingPolicy() {
		return ClosingPolicy.FIFO;
	}

}
