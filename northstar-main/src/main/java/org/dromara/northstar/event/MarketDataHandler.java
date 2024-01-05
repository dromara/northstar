package org.dromara.northstar.event;

import java.util.concurrent.ExecutorService;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.event.AbstractEventHandler;
import org.dromara.northstar.common.event.GenericEventHandler;
import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.data.IMarketDataRepository;

/**
 * 处理K线数据持久化
 * @author KevinHuangwl
 *
 */
public class MarketDataHandler extends AbstractEventHandler implements GenericEventHandler{

	private IMarketDataRepository mdRepo;
	
	private ExecutorService exec = CommonUtils.newThreadPerTaskExecutor(getClass());
	
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
