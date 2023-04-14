package org.dromara.northstar.main.handler.internal;

import java.time.LocalDate;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.StringUtils;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.event.AbstractEventHandler;
import org.dromara.northstar.common.event.GenericEventHandler;
import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.gateway.api.utils.MarketDataRepoFactory;

import xyz.redtorch.pb.CoreField.BarField;

/**
 * 处理K线数据持久化
 * @author KevinHuangwl
 *
 */
public class MarketDataHandler extends AbstractEventHandler implements GenericEventHandler{

	private MarketDataRepoFactory mdRepoFactory;
	
	private ThreadPoolExecutor exec = new ThreadPoolExecutor(5, 10, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(500));
	
	public MarketDataHandler(MarketDataRepoFactory mdRepoFactory) {
		this.mdRepoFactory = mdRepoFactory;
	}
	
	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return eventType == NorthstarEventType.BAR;
	}

	@Override
	protected void doHandle(NorthstarEvent e) {
		if(e.getData() instanceof BarField bar && bar.getActionDay().equals(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))) {
			ChannelType channelType = ChannelType.valueOf(bar.getGatewayId());
			if(channelType != ChannelType.SIM && StringUtils.equals(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER), bar.getActionDay())) {
				exec.execute(() -> mdRepoFactory.getInstance(channelType).insert(bar)); 
			}
		}
	}

}
