package org.dromara.northstar.module;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.ModuleIndicatorDescription;
import org.dromara.northstar.common.model.TimeSeriesValue;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.IndicatorValueUpdateHelper;
import org.dromara.northstar.indicator.model.Num;
import org.dromara.northstar.indicator.model.RingArray;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChartBuffer {
	
	private final int bufSize;
	
    private Map<Contract, RingArray<Bar>> barBuffMap = new HashMap<>();

    private Map<Indicator, RingArray<Num>> indicatorBufMap = new HashMap<>();
    
    public ChartBuffer(int size){
    	bufSize = size;
    }

    public synchronized void update(Bar bar, Collection<IndicatorValueUpdateHelper> hlps){
        barBuffMap.computeIfAbsent(bar.contract(), c -> new RingArray<>(bufSize));
        barBuffMap.get(bar.contract()).update(bar, false);
        hlps.stream()
	        .map(IndicatorValueUpdateHelper::getIndicator)
	        .filter(indicator -> indicator.getConfiguration().contract().equals(bar.contract()))
	        .filter(indicator -> indicator.getConfiguration().visible())
	        .forEach(indicator -> {
	        	indicatorBufMap.computeIfAbsent(indicator, in -> new RingArray<>(bufSize));
	            if(indicator.isReady() && (indicator.getConfiguration().ifPlotPerBar() || !indicator.get(0).unstable())) {
	            	indicatorBufMap.get(indicator).update(indicator.get(0), indicator.get(0).unstable());
	            } else {
	            	indicatorBufMap.get(indicator).update(Num.NaN(), false);
	            }
	        });
    }
    
    public Map<Contract, List<Bar>> getBarBuffers(){
		return barBuffMap.entrySet()
				.stream()
				.collect(HashMap::new, (m,e) -> m.put(e.getKey(), e.getValue().toList().reversed()), HashMap::putAll);
	}

    public List<ModuleIndicatorDescription> getIndicatorDescriptions(){
		return indicatorBufMap.entrySet().stream()
				.map(e -> ModuleIndicatorDescription.builder()
						.name(e.getKey().getConfiguration().indicatorID())
						.contract(ContractSimpleInfo.from(e.getKey().getConfiguration().contract()))
						.style(e.getKey().getConfiguration().lineStyle())
						.values(e.getValue().toList().reversed().stream().map(n -> new TimeSeriesValue(n.value(), n.timestamp())).toList())
						.build())
				.toList();
	}
}
