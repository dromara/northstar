package org.dromara.northstar.support.utils.bar;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.model.PeriodSegment;

import xyz.redtorch.pb.CoreField.BarField;

/**
 * 月线合成器
 * @author KevinHuangwl
 *
 */
public class MonthlyBarMerger extends BarMerger{
	
	private final int numOfMonthPerBar;
	

	public MonthlyBarMerger(int numOfMonthPerBar, Contract contract) {
		super(0, contract);
		this.numOfMonthPerBar = numOfMonthPerBar;
	}
	
	@Override
	public synchronized void onBar(Bar bar) {
		if(!contract.equals(bar.contract())) {
			return;
		}
		
	}
	
}
