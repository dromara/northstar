package org.dromara.northstar.event;

import java.util.concurrent.ExecutorService;
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

import xyz.redtorch.pb.CoreEnum.BarCycleEnum;

/**
 * 处理K线数据持久化
 * @author KevinHuangwl
 *
 */
public class MarketDataPersistanceHandler extends AbstractEventHandler implements GenericEventHandler, InitializingBean, DisposableBean{

	private IMarketDataRepository mdRepo;
	
	private AtomicBoolean shutdown = new AtomicBoolean();
	
	private ExecutorService exec;
	
	public MarketDataPersistanceHandler(IMarketDataRepository mdRepo) {
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
		if(e.getData() instanceof Bar bar) {
			if(bar.barPeriod() != BarCycleEnum.B_1Min
					|| bar.channelType() == ChannelType.PLAYBACK
					|| bar.channelType() == ChannelType.SIM) {
				return;
			}
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
