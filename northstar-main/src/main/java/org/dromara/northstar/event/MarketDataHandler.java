package org.dromara.northstar.event;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.event.AbstractEventHandler;
import org.dromara.northstar.common.event.GenericEventHandler;
import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.data.IMarketDataRepository;

/**
 * 处理K线数据持久化
 * @author KevinHuangwl
 *
 */
public class MarketDataHandler extends AbstractEventHandler implements GenericEventHandler{

	private IMarketDataRepository mdRepo;
	
	private ThreadPoolExecutor exec = new ThreadPoolExecutor(5, 10, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(500));
	
	public MarketDataHandler(IMarketDataRepository mdRepo) {
		this.mdRepo = mdRepo;
	}
	
	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return eventType == NorthstarEventType.BAR;
	}

	@Override
	protected void doHandle(NorthstarEvent e) {
		if(e.getData() instanceof Bar bar && System.currentTimeMillis() - bar.actionTimestamp() < 120000 && bar.channelType() != ChannelType.SIM) {
			exec.execute(() -> mdRepo.insert(bar)); 
		}
	}

}
