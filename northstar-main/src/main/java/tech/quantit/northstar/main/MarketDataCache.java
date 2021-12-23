package tech.quantit.northstar.main;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import tech.quantit.northstar.main.persistence.MarketDataRepository;
import tech.quantit.northstar.main.persistence.po.MinBarDataPO;

@Component
public class MarketDataCache {
	
	@Autowired
	MarketDataRepository mdRepo;
	
	private ConcurrentLinkedQueue<MinBarDataPO> cacheQ = new ConcurrentLinkedQueue<>();
	
	public void save(MinBarDataPO po) {
		cacheQ.offer(po);
	}

	public void writeDisk() {
		List<MinBarDataPO> list = new LinkedList<>();
		while(!cacheQ.isEmpty()) {
			list.add(cacheQ.poll());
		}
		mdRepo.insertMany(list);
	}
}
