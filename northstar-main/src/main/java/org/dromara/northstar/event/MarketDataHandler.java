package org.dromara.northstar.event;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.event.AbstractEventHandler;
import org.dromara.northstar.common.event.GenericEventHandler;
import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.data.IMarketDataRepository;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * 处理K线数据持久化
 * @author KevinHuangwl
 *
 */
public class MarketDataHandler extends AbstractEventHandler implements GenericEventHandler, InitializingBean, DisposableBean{

	private IMarketDataRepository mdRepo;
	
	private AtomicBoolean shutdown = new AtomicBoolean();
	
	private ExecutorService exec;
	
	public MarketDataHandler(IMarketDataRepository mdRepo) {
		this.mdRepo = mdRepo;
	}
	
	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return eventType == NorthstarEventType.BAR;
	}

	@Override
	protected void doHandle(NorthstarEvent e) {
		if(shutdown.get()) {
			return;
		}
		if(e.getData() instanceof Bar bar && System.currentTimeMillis() - bar.actionTimestamp() < TimeUnit.MINUTES.toMillis(5) && bar.channelType() != ChannelType.SIM) {
			exec.execute(() -> mdRepo.insert(bar)); 
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		exec = CommonUtils.newThreadPerTaskExecutor(getClass());
	}
	
	@Override
	public void destroy() throws Exception {
		shutdown.set(true);
		exec.close();
	}
}
