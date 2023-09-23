package org.dromara.northstar.common.constant;

import org.dromara.northstar.common.exception.TradeException;
import org.dromara.northstar.common.model.Tuple;

import lombok.Getter;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.PositionField;

/**
 * 平仓策略
 * @author KevinHuangwl
 *
 */
public enum ClosingPolicy {

	/**
	 * 按开仓的时间顺序平仓
	 */
	FIRST_IN_FIRST_OUT("先开先平") {

		@Override
		public Tuple<OffsetFlagEnum, Integer> resolve(SignalOperation operation, PositionField position, int signalVol) {
			if(operation.isOpen())
				return Tuple.of(OffsetFlagEnum.OF_Open, signalVol);
			if(position.getYdPosition() - position.getYdFrozen() > 0)
				return Tuple.of(OffsetFlagEnum.OF_CloseYesterday, Math.min(position.getYdPosition() - position.getYdFrozen(), signalVol));
			if(position.getTdPosition() - position.getTdFrozen() > 0)
				return Tuple.of(OffsetFlagEnum.OF_CloseToday, Math.min(position.getTdPosition() - position.getTdFrozen(), signalVol));
			throw new TradeException(String.format("%s没有足够持仓可平仓，平仓数：%d手，实际可用：%d手", position.getContract().getName(), signalVol, position.getPosition() - position.getFrozen()));
		}

	},
	/**
	 * 按开仓的时间倒序平仓
	 */
	FIRST_IN_LAST_OUT("平今优先") {

		@Override
		public Tuple<OffsetFlagEnum, Integer> resolve(SignalOperation operation, PositionField position, int signalVol) {
			if(operation.isOpen())
				return Tuple.of(OffsetFlagEnum.OF_Open, signalVol);
			if(position.getTdPosition() - position.getTdFrozen() > 0)
				return Tuple.of(OffsetFlagEnum.OF_CloseToday, Math.min(position.getTdPosition() - position.getTdFrozen(), signalVol));
			if(position.getYdPosition() - position.getYdFrozen() > 0)
				return Tuple.of(OffsetFlagEnum.OF_CloseYesterday, Math.min(position.getYdPosition() - position.getYdFrozen(), signalVol));
			throw new IllegalStateException(String.format("%s没有足够持仓可平仓，平仓数：%d手，实际可用：%d手", position.getContract().getName(), signalVol, position.getPosition() - position.getFrozen()));
		}
		
	},
	/**
	 * 优先平掉历史持仓，对冲锁仓今天的持仓
	 * 注意：这里只有锁仓逻辑，没有解锁逻辑
	 */
	CLOSE_NONTODAY_HEGDE_TODAY("平昨锁今") {

		@Override
		public Tuple<OffsetFlagEnum, Integer> resolve(SignalOperation operation, PositionField position, int signalVol) {
			if(operation.isClose() && position.getYdPosition() - position.getYdFrozen() > 0)
				return Tuple.of(OffsetFlagEnum.OF_CloseYesterday, Math.min(position.getYdPosition() - position.getYdFrozen(), signalVol));
			return Tuple.of(OffsetFlagEnum.OF_Open, signalVol);
		}

	};

	@Getter
	private String name;
	private ClosingPolicy(String name) {
		this.name = name;
	}
	
	public abstract Tuple<OffsetFlagEnum, Integer> resolve(SignalOperation operation, PositionField position, int signalVol);
	
}
