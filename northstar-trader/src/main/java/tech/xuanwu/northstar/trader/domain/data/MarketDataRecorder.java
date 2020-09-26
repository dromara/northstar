package tech.xuanwu.northstar.trader.domain.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import tech.xuanwu.northstar.constant.CommonConstant;
import tech.xuanwu.northstar.gateway.FastEventEngine;
import tech.xuanwu.northstar.gateway.FastEventEngine.EventType;
import tech.xuanwu.northstar.gateway.FastEventEngine.FastEvent;
import tech.xuanwu.northstar.gateway.FastEventEngine.FastEventHandler;
import tech.xuanwu.northstar.persistance.BarRepo;
import tech.xuanwu.northstar.persistance.po.Bar;
import tech.xuanwu.northstar.persistance.po.Tick;
import xyz.redtorch.common.util.bar.BarGenerator;
import xyz.redtorch.pb.CoreField.TickField;

@Component
public class MarketDataRecorder implements FastEventHandler{

	@Autowired
	private FastEventEngine feEngine;
	
	private Map<String, List<TickField>> tickMap = new HashMap<>(200);
	private Map<String, BarGenerator> barGenMap = new HashMap<>(200);
	
	@Autowired
	private BarRepo barRepo;
	
	@PostConstruct
	public void init() {
		feEngine.addHandler(this);
	}

	@Override
	public void onEvent(FastEvent event, long sequence, boolean endOfBatch) throws Exception {
		if(event.getEventType() != EventType.TICK) {
			return;
		}
		
		TickField tick = (TickField) event.getObj();
		
		//由于Disruptor的线程模型是一个handler一个线程，因此不会出现线程安全问题
		String unifiedSymbol = tick.getUnifiedSymbol();
		if(!tickMap.containsKey(unifiedSymbol)) {
			// 一秒最多六个tick
			tickMap.put(unifiedSymbol, new ArrayList<>(360));
			barGenMap.put(unifiedSymbol, new BarGenerator((bar)->{
				Bar barPO = Bar.convertFrom(bar);
				List<TickField> tickList = tickMap.get(unifiedSymbol);
				// 仅保存指数合约的TICK数据，为了节省储存空间
				if(unifiedSymbol.contains(CommonConstant.INDEX_SUFFIX)) {					
					barPO.setMinTicks(tickList.stream().map(t -> Tick.convertFrom(t)).collect(Collectors.toList()));
				}
				tickList.clear();
				barRepo.save(barPO);
			}));
		}
		
		barGenMap.get(unifiedSymbol).updateTick(tick);
		tickMap.get(unifiedSymbol).add(tick);
		
	}

	
}
