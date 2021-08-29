package tech.xuanwu.northstar.main.manager;

import java.util.LinkedList;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.main.persistence.MarketDataRepository;
import tech.xuanwu.northstar.main.persistence.po.MinBarDataPO;

/**
 * 用于缓存即将持久化的bar数据
 * @author KevinHuangwl
 *
 */
@Slf4j
public class BarBufferManager {

	private volatile LinkedList<MinBarDataPO> bufData = new LinkedList<>();
	
	private MarketDataRepository mdRepo; 
	
	public BarBufferManager(MarketDataRepository mdRepo){
		this.mdRepo = mdRepo;
	}
	
	public synchronized void addBar(MinBarDataPO po) {
		bufData.add(po);
	}
	
	public synchronized void saveAndClear() {
		if(bufData.size() == 0) {
			log.info("没有数据需要保存");
			return;
		}
		LinkedList<MinBarDataPO> bufDataTemp = bufData;
		bufData = new LinkedList<>();
		mdRepo.insertMany(bufDataTemp);
	}
}
