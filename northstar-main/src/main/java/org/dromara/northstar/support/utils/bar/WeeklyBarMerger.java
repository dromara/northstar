package org.dromara.northstar.support.utils.bar;

import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;

/**
 * 周线合成器
 * @author KevinHuangwl
 *
 */
public class WeeklyBarMerger extends BarMerger{
	
	private final int numOfWeekPerBar;
	
	public WeeklyBarMerger(int numOfWeekPerBar, Contract contract) {
		super(0, contract);
		this.numOfWeekPerBar = numOfWeekPerBar;
	}

	@Override
	public synchronized void onBar(Bar bar) {
	}
	
	
}
