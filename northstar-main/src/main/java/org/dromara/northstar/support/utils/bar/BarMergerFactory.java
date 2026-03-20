package org.dromara.northstar.support.utils.bar;

import org.dromara.northstar.common.model.core.Contract;

import xyz.redtorch.pb.CoreEnum.BarCycleEnum;

public class BarMergerFactory {
	
	private BarMergerFactory() {}

	public static BarMerger newInstance(BarCycleEnum barPeriod, Contract contract) {
		return switch(barPeriod) {
			case B_1Min -> new BarMerger(1, contract);
			case B_3Min -> new BarMerger(3, contract);
			case B_5Min -> new BarMerger(5, contract);
			case B_10Min -> new BarMerger(10, contract);
			case B_15Min -> new BarMerger(15, contract);
			case B_30Min -> new BarMerger(30, contract);
			case B_1Hr -> new BarMerger(60, contract);
			case B_2Hr -> new BarMerger(120, contract);
			case B_4Hr -> new BarMerger(240, contract);
			case B_1Day -> new DailyBarMerger(contract);
			case B_1Wk -> new WeeklyBarMerger(contract);
			default -> throw new IllegalArgumentException("Unexpected value: " + barPeriod);
		};
	}
}
