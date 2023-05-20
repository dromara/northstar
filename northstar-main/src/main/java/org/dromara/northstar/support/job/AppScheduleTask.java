package org.dromara.northstar.support.job;

import javax.transaction.Transactional;

import org.dromara.northstar.data.jdbc.MarketDataRepository;
import org.dromara.northstar.gateway.IMarketCenter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 主程序定时检查任务
 * @author KevinHuangwl
 *
 */
@Slf4j
@Component
public class AppScheduleTask {
	
	@Autowired
	private IMarketCenter mktCenter;
	
	@Autowired
	private MarketDataRepository mdRepo;

	/**
	 * K线数据合成检查
	 */
	@Scheduled(cron="0 0/1 * ? * 1-6")
	public void sectionFinishUp() {
		mktCenter.endOfMarketTime();
		log.debug("K线数据合成检查");
	}
	
	/**
	 * 移除过期行情数据
	 */
	@Scheduled(cron="0 30 20 ? * 1-5")
	@Transactional
	public void removeExpiredData() {
		mdRepo.deleteByExpiredAtBefore(System.currentTimeMillis());
		log.debug("移除过期行情数据");
	}
}
