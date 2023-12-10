package org.dromara.northstar.common.constant;

import org.dromara.northstar.common.exception.TradeException;
import org.dromara.northstar.common.model.Tuple;
import org.dromara.northstar.common.model.core.Position;

import lombok.Getter;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;

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
		public Tuple<OffsetFlagEnum, Integer> resolve(SignalOperation operation, Position position, int signalVol) {
			if(operation.isOpen())
				return Tuple.of(OffsetFlagEnum.OF_Open, signalVol);
			if(position.ydPosition() - position.ydFrozen() > 0)
				return Tuple.of(OffsetFlagEnum.OF_CloseYesterday, Math.min(position.ydPosition() - position.ydFrozen(), signalVol));
			if(position.tdPosition() - position.tdFrozen() > 0)
				return Tuple.of(OffsetFlagEnum.OF_CloseToday, Math.min(position.tdPosition() - position.tdFrozen(), signalVol));
			throw new TradeException(String.format("%s没有足够持仓可平仓，平仓数：%d手，实际可用：%d手", position.contract().name(), signalVol, position.position() - position.frozen()));
		}

	},
	/**
	 * 按开仓的时间倒序平仓
	 */
	FIRST_IN_LAST_OUT("平今优先") {

		@Override
		public Tuple<OffsetFlagEnum, Integer> resolve(SignalOperation operation, Position position, int signalVol) {
			if(operation.isOpen())
				return Tuple.of(OffsetFlagEnum.OF_Open, signalVol);
			if(position.tdPosition() - position.tdFrozen() > 0)
				return Tuple.of(OffsetFlagEnum.OF_CloseToday, Math.min(position.tdPosition() - position.tdFrozen(), signalVol));
			if(position.ydPosition() - position.ydFrozen() > 0)
				return Tuple.of(OffsetFlagEnum.OF_CloseYesterday, Math.min(position.ydPosition() - position.ydFrozen(), signalVol));
			throw new IllegalStateException(String.format("%s没有足够持仓可平仓，平仓数：%d手，实际可用：%d手", position.contract().name(), signalVol, position.position() - position.frozen()));
		}
		
	},
	/**
	 * 优先平掉历史持仓，对冲锁仓今天的持仓
	 * 注意：这里只有锁仓逻辑，没有解锁逻辑
	 */
	CLOSE_NONTODAY_HEGDE_TODAY("平昨锁今") {

		@Override
		public Tuple<OffsetFlagEnum, Integer> resolve(SignalOperation operation, Position position, int signalVol) {
			if(operation.isClose() && position.ydPosition() - position.ydFrozen() > 0)
				return Tuple.of(OffsetFlagEnum.OF_CloseYesterday, Math.min(position.ydPosition() - position.ydFrozen(), signalVol));
			return Tuple.of(OffsetFlagEnum.OF_Open, signalVol);
		}

	};

	@Getter
	private String name;
	private ClosingPolicy(String name) {
		this.name = name;
	}
	
	public abstract Tuple<OffsetFlagEnum, Integer> resolve(SignalOperation operation, Position position, int signalVol);
	
}
