package tech.quantit.northstar.strategy.api.utils.bar;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import cn.hutool.core.date.LocalDateTimeUtil;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.gateway.api.domain.contract.Contract;
import xyz.redtorch.pb.CoreField.BarField;

/**
 * 周线合成器
 * @author KevinHuangwl
 *
 */
public class WeeklyBarMerger extends BarMerger{
	
	private final int numOfWeekPerBar;
	
	private Set<String> yearWeekSet = new HashSet<>();

	public WeeklyBarMerger(int numOfWeekPerBar, Contract contract, Consumer<BarField> callback) {
		super(0, contract, callback);
		this.numOfWeekPerBar = numOfWeekPerBar;
	}

	@Override
	public void onBar(BarField bar) {
		if(!StringUtils.equals(bar.getUnifiedSymbol(), unifiedSymbol)) {
			return;
		}
		String ywk = toYearWeek(LocalDate.parse(bar.getTradingDay(), DateTimeConstant.D_FORMAT_INT_FORMATTER));
		if(Objects.nonNull(barBuilder) && yearWeekSet.size() == numOfWeekPerBar && !yearWeekSet.contains(ywk)) {
			doGenerate();
		}
		
		yearWeekSet.add(ywk);
		
		if(Objects.isNull(barBuilder)) {
			barBuilder = bar.toBuilder();
			return;
		}
		
		doMerge(bar);
	}
	
	// 2022年第2周，表达为202202
	private String toYearWeek(LocalDate date) {
		return String.format("%d%02d", date.getYear(), LocalDateTimeUtil.weekOfYear(date));
	}
	
	@Override
	protected void doGenerate() {
		callback.accept(barBuilder.build());
		barBuilder = null;
		yearWeekSet.clear();
	}
}
