package tech.quantit.northstar.strategy.api.utils.bar;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.StringUtils;

import tech.quantit.northstar.gateway.api.domain.contract.Contract;
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
		
		if(Objects.nonNull(barBuilder) && tradingDaySet.size() == numOfDayPerBar && !tradingDaySet.contains(bar.getTradingDay())) {
			doGenerate();
		}
		
		tradingDaySet.add(bar.getTradingDay());

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
		tradingDaySet.clear();
	}
}
