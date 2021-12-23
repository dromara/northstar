package tech.quantit.northstar.main.handler.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import tech.quantit.northstar.main.MarketDataCache;
import tech.quantit.northstar.main.persistence.po.MinBarDataPO;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 负责处理行情持久化
 * 
 * @author KevinHuangwl
 *
 */
public class MarketBarDataPersistenceHandler {

	private static final int DEFAULT_SIZE = 4096;
	
	private static final int DEFAULT_LEN = 300;
	
	private Map<String, Queue<TickField>> ticksQMap = new HashMap<>(DEFAULT_SIZE);
	
	private MarketDataCache cache;
	
	public MarketBarDataPersistenceHandler(MarketDataCache cache) {
		this.cache = cache;
	}
	
	public void onTick(TickField tick) {
		if(!ticksQMap.containsKey(tick.getUnifiedSymbol())) {
			ticksQMap.put(tick.getUnifiedSymbol(), new LinkedList<>());
		}
		ticksQMap.get(tick.getUnifiedSymbol()).offer(tick);
	}
	
	public void onBar(BarField bar) {
		long barTime = bar.getActionTimestamp();
		Queue<TickField> ticksQ = ticksQMap.get(bar.getUnifiedSymbol());
		List<TickField> minTicks = new ArrayList<>(DEFAULT_LEN);
		while(!ticksQ.isEmpty() && ticksQ.peek().getActionTimestamp() <= barTime + 60000) {
			minTicks.add(ticksQ.poll());
		}
		cache.save(MinBarDataPO.builder()
					.gatewayId(bar.getGatewayId())
					.unifiedSymbol(bar.getUnifiedSymbol())
					.barData(bar.toByteArray())
					.ticksData(minTicks.stream().map(TickField::toByteArray).toList())
					.updateTime(barTime + 60000)
					.tradingDay(bar.getTradingDay())
					.build());
	}
	
}
