package tech.quantit.northstar.main.handler.internal;

import java.time.LocalDate;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.common.event.AbstractEventHandler;
import tech.quantit.northstar.common.event.GenericEventHandler;
import tech.quantit.northstar.common.event.NorthstarEvent;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.gateway.api.utils.MarketDataRepoFactory;
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
			exec.execute(() -> mdRepoFactory.getInstance(bar.getGatewayId()).insert(bar)); 
		}
	}

}
