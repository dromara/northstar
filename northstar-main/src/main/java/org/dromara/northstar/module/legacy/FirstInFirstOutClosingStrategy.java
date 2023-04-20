package org.dromara.northstar.module.legacy;

import org.dromara.northstar.common.constant.ClosingPolicy;
import org.dromara.northstar.common.constant.SignalOperation;
import org.dromara.northstar.strategy.ClosingStrategy;

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
			String msg = String.format("持仓数量：%d，冻结数据：%d", position.getPosition(), position.getFrozen());
			throw new IllegalStateException("没有足够持仓可用：" + msg);
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
