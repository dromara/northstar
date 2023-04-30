package org.dromara.northstar.support.utils.bar;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.gateway.Contract;

import xyz.redtorch.pb.CoreField.BarField;

/**
 * 月线合成器
 * @author KevinHuangwl
 *
 */
public class MonthlyBarMerger extends BarMerger{
	
	private final int numOfMonthPerBar;
	
	private Set<String> yMonthSet = new HashSet<>();

	public MonthlyBarMerger(int numOfMonthPerBar, Contract contract, BiConsumer<BarMerger, BarField> callback) {
		super(0, contract, callback);
		this.numOfMonthPerBar = numOfMonthPerBar;
	}

	@Override
	public void onBar(BarField bar) {
		if(!StringUtils.equals(bar.getUnifiedSymbol(), unifiedSymbol)) {
			return;
		}
		
		String yearMonth = toYearMonth(LocalDate.parse(bar.getTradingDay(), DateTimeConstant.D_FORMAT_INT_FORMATTER));
		
		if(Objects.nonNull(barBuilder) && yMonthSet.size() == numOfMonthPerBar && !yMonthSet.contains(yearMonth)) {
			doGenerate();
		}
		
		yMonthSet.add(yearMonth);
		
		if(Objects.isNull(barBuilder)) {
			barBuilder = bar.toBuilder();
			return;
		}
		
		doMerge(bar);
	}

	@Override
	protected void doGenerate() {
		callback.accept(this, barBuilder.build());
		barBuilder = null;
		yMonthSet.clear();
	}

	private String toYearMonth(LocalDate date) {
		return String.format("%d%02d", date.getYear(), date.getMonthValue());
	}
}
