package tech.xuanwu.northstar.trader.domain.event;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import tech.xuanwu.northstar.constant.CommonConstant;
import tech.xuanwu.northstar.gateway.FastEventEngine;
import tech.xuanwu.northstar.gateway.FastEventEngine.EventType;
import tech.xuanwu.northstar.gateway.FastEventEngine.FastEvent;
import tech.xuanwu.northstar.gateway.FastEventEngine.FastEventHandler;
import tech.xuanwu.northstar.trader.domain.broadcast.MessageEngine;
import tech.xuanwu.northstar.trader.domain.contract.IndexContractMaker;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.NoticeField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 网关事件广播台
 * @author kevinhuangwl
 *
 */
@Component
public class GatewayBroadcaster implements FastEventHandler{

	@Autowired
	private MessageEngine msgEngine;
	
	@Autowired
	private FastEventEngine feEngine;
	
	@Autowired
	private IndexContractMaker indexContractMaker;
	
	private volatile long lastTickTimestamp;
	
	//用于缓存下单记录
	private ConcurrentHashMap<String, OrderField> orderCacheMap = new ConcurrentHashMap<>(100);
	//用于缓存成交记录
	private ConcurrentHashMap<String, TradeField> tradeCacheMap = new ConcurrentHashMap<>(100);

	ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);
	
	@PostConstruct
	private void register() {
		feEngine.addHandler(this);
		
		//定时同步下单记录与成交记录
		exec.scheduleAtFixedRate(()->{
			orderCacheMap.forEach((id, order) -> msgEngine.emitOrder(order));
			tradeCacheMap.forEach((id, trade) -> msgEngine.emitTrade(trade));
		}, 10, 5, TimeUnit.SECONDS);
	}
	
	@Override
	public void onEvent(FastEvent event, long sequence, boolean endOfBatch) throws Exception {
		EventType eventType = event.getEventType();
		Object obj = event.getObj();
		switch(eventType) {
		case TICK:
			TickField tick = (TickField) obj;
			msgEngine.emitTick(tick);
			lastTickTimestamp = System.currentTimeMillis();
			indexContractMaker.updateTick(tick);
			break;
		case BAR:
			BarField bar = (BarField) obj;
			msgEngine.emitBar(bar);
			break;
		case ACCOUNT:
			AccountField account = (AccountField) obj;
			msgEngine.emitAccount(account);
			break;
		case POSITION:
			PositionField position = (PositionField) obj;
			msgEngine.emitPosition(position);
			break;
		case TRADE:
			TradeField trade = (TradeField) obj;
			msgEngine.emitTrade(trade);
			tradeCacheMap.put(trade.getTradeId(), trade);
			break;
		case ORDER:
			OrderField order = (OrderField) obj;
			msgEngine.emitOrder(order);
			orderCacheMap.put(order.getOrderId(), order);
			break;
		case CONTRACT:
			ContractField contract = (ContractField) obj;
			msgEngine.emitContract(contract);
			break;
		case NOTICE:
			NoticeField notice = (NoticeField) obj;
			msgEngine.emitNotice(notice);
		default:
			//只响应以上事件
			break;
		}
	}

	public boolean isMarkDataOnRunning() {
		return System.currentTimeMillis() - lastTickTimestamp < 1000;
	}
}
