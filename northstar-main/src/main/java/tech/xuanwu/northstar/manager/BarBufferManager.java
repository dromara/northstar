package tech.xuanwu.northstar.manager;

import java.util.LinkedList;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.persistence.MarketDataRepository;
import tech.xuanwu.northstar.persistence.po.MinBarDataPO;

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
			log.debug("没有数据需要保存");
			return;
		}
		LinkedList<MinBarDataPO> bufDataTemp = bufData;
		bufData = new LinkedList<>();
		mdRepo.insertMany(bufDataTemp);
	}
}
