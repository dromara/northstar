package org.dromara.northstar.support.utils.bar;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.gateway.Contract;
import org.dromara.northstar.gateway.model.PeriodSegment;

import xyz.redtorch.pb.CoreField.BarField;

/**
 * 日线合成器
 * @author KevinHuangwl
 *
 */
public class DailyBarMerger extends BarMerger{

	private final int numOfDayPerBar;
	
	private Set<String> tradingDaySet = new HashSet<>();
	
	public DailyBarMerger(int numOfDayPerBar, Contract contract, BiConsumer<BarMerger, BarField> callback) {
		super(0, contract, callback);
		this.numOfDayPerBar = numOfDayPerBar;
	}

	@Override
	public void onBar(BarField bar) {
		if(!StringUtils.equals(bar.getUnifiedSymbol(), unifiedSymbol)) {
			return;
		}
		
		tradingDaySet.add(bar.getTradingDay());
		
		if(Objects.nonNull(barBuilder) && tradingDaySet.size() == numOfDayPerBar && isLastBarOfDay(bar)) {
			doMerge(bar);
			doGenerate();
			return;
		}
		
		if(Objects.isNull(barBuilder)) {
			barBuilder = bar.toBuilder();
			return;
		}
		
		doMerge(bar);
	}

	private boolean isLastBarOfDay(BarField bar) {
		List<PeriodSegment> segments = contract.tradeTimeDefinition().tradeTimeSegments();
		LocalTime time = segments.get(segments.size() - 1).endOfSegment();
		return LocalTime.parse(bar.getActionTime(), DateTimeConstant.T_FORMAT_FORMATTER).equals(time);
	}

	@Override
	protected void doGenerate() {
		callback.accept(this, barBuilder.build());
		barBuilder = null;
		tradingDaySet.clear();
	}
}
