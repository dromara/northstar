package tech.quantit.northstar.main;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import tech.quantit.northstar.gateway.api.MarketDataBuffer;
import tech.quantit.northstar.main.persistence.MarketDataRepository;
import tech.quantit.northstar.main.persistence.po.MinBarDataPO;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

@Component
public class MarketDataCache implements MarketDataBuffer{
	
	@Autowired
	MarketDataRepository mdRepo;
	
	private ConcurrentLinkedQueue<MinBarDataPO> cacheQ = new ConcurrentLinkedQueue<>();
	
	public void writeDisk() {
		List<MinBarDataPO> list = new LinkedList<>();
		while(!cacheQ.isEmpty()) {
			list.add(cacheQ.poll());
		}
		if(!list.isEmpty()) {		
			mdRepo.insertMany(list);
		}
	}

	@Override
	public void save(BarField bar, List<TickField> ticks) {
		long barTime = bar.getActionTimestamp();
		cacheQ.offer(MinBarDataPO.builder()
				.gatewayId(bar.getGatewayId())
				.unifiedSymbol(bar.getUnifiedSymbol())
				.barData(bar.toByteArray())
				.ticksData(ticks.stream().map(TickField::toByteArray).toList())
				.updateTime(barTime + 60000)
				.tradingDay(bar.getTradingDay())
				.build());
	}
}
