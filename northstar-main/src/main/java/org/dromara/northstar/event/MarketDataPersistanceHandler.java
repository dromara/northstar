package org.dromara.northstar.event;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.event.AbstractEventHandler;
import org.dromara.northstar.common.event.GenericEventHandler;
import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.data.IMarketDataRepository;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * 处理K线数据持久化
 * @author KevinHuangwl
 *
 */
public class MarketDataPersistanceHandler extends AbstractEventHandler implements GenericEventHandler, InitializingBean, DisposableBean{

	private IMarketDataRepository mdRepo;
	
	private AtomicBoolean shutdown = new AtomicBoolean();
	
	private ExecutorService exec;
	
	protected Table<Contract, Interval, Long> barFilterMap = HashBasedTable.create();
	
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
			if(bar.interval() != Interval.M1 || bar.unstable() || bar.channelType() != ChannelType.CTP) {
				return;
			}
			if(barFilterMap.contains(bar.contract(), bar.interval()) && barFilterMap.get(bar.contract(), bar.interval()) >= bar.actionTimestamp()) {
				//过滤掉可能存在的重复数据
				return;
			}
			barFilterMap.put(bar.contract(), bar.interval(), bar.actionTimestamp());
			// 仅保存CTP的1分钟K线数据
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
