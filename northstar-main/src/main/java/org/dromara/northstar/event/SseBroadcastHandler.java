package org.dromara.northstar.event;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

import org.dromara.northstar.common.constant.TickType;
import org.dromara.northstar.common.event.AbstractEventHandler;
import org.dromara.northstar.common.event.GenericEventHandler;
import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.model.core.Account;
import org.dromara.northstar.common.model.core.Notice;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Position;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.support.utils.EventEmitterHolder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

import cn.hutool.core.codec.Base64;
import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

@Slf4j
public class SseBroadcastHandler extends AbstractEventHandler implements GenericEventHandler{

	protected ConcurrentMap<String, OrderField> cacheOrders = new ConcurrentHashMap<>();
	protected ConcurrentMap<String, TradeField> cacheTrades = new ConcurrentHashMap<>();
	protected ConcurrentMap<String, TickField> cacheTicks = new ConcurrentHashMap<>();
	
	private static final Set<NorthstarEventType> TARGET_TYPE = EnumSet.of(
			NorthstarEventType.TICK, 
			NorthstarEventType.ACCOUNT,
			NorthstarEventType.POSITION,
			NorthstarEventType.TRADE,
			NorthstarEventType.ORDER,
			NorthstarEventType.NOTICE
	);
	
	private EventEmitterHolder emitterHolder;
	
	private ExecutorService exec;
	
	public SseBroadcastHandler(EventEmitterHolder emitterHolder) {
		this.emitterHolder = emitterHolder;
		this.exec = CommonUtils.newThreadPerTaskExecutor(getClass());
	}
	
	private SseEventBuilder event() {
		return SseEmitter.event();
	}

	private void doEmit(NorthstarEvent event) {
		try {
			/* BAR事件的广播参考BarAggregationHandler */
			if(event.getData() instanceof Tick t) {
				if(t.type() == TickType.PLAYBACK_DUMMY_TICK || t.type() == TickType.INFO_TICK) {
					return; // 忽略 PLAYBACK_DUMMY_TICK
				}
				TickField tick = t.toTickField();
				log.trace("TICK数据分发：[{} {} {} {} 总仓：{} 仓：{} 总量：{} 量：{} 价：{} 类型：{}]", 
						tick.getUnifiedSymbol(), tick.getActionDay(), tick.getActionTime(), tick.getActionTimestamp(),
						tick.getOpenInterest(), tick.getOpenInterestDelta(), tick.getVolume(), tick.getVolumeDelta(), tick.getLastPrice(), t.type());
				String contractId = t.contract().contractId();
				cacheTicks.put(contractId, tick);
			} else if(event.getData() instanceof Account acc) {
				AccountField account = acc.toAccountField();
				log.trace("账户信息分发: [{} {} {}]", account.getAccountId(), account.getGatewayId(), account.getBalance());
				emitterHolder.send(event().name(NorthstarEventType.ACCOUNT.toString()).data(Base64.encode(account.toByteArray())));
			} else if(event.getData() instanceof Position pos) {
				PositionField position = pos.toPositionField();
				log.trace("持仓信息分发: [{} {} {}]", position.getAccountId(), position.getGatewayId(), position.getPositionId());
				emitterHolder.send(event().name(NorthstarEventType.POSITION.toString()).data(Base64.encode(position.toByteArray())));
			} else if(event.getData() instanceof Order od) {
				OrderField order = od.toOrderField();
				cacheOrders.put(order.getOrderId(), order);
				log.trace("委托信息分发：[{}/{} 状态：{}/{} 委托量：{} 已成交：{}]", order.getOriginOrderId(), order.getOrderId(), 
						order.getOrderStatus(), order.getOrderStatus(), order.getTotalVolume(), order.getTradedVolume());
				emitterHolder.send(event().name(NorthstarEventType.ORDER.toString()).data(Base64.encode(order.toByteArray())));
			} else if(event.getData() instanceof Trade tr) {
				TradeField trade = tr.toTradeField();
				cacheTrades.put(trade.getTradeId(), trade);
				log.trace("成交信息分发：[{}/{} 成交量：{}]", trade.getOriginOrderId(), trade.getOrderId(), trade.getVolume());
				emitterHolder.send(event().name(NorthstarEventType.TRADE.toString()).data(Base64.encode(trade.toByteArray())));
			} else if (event.getData() instanceof Notice notice) {
				emitterHolder.send(SseEmitter.event().name(notice.status().toString()).data(notice.content()));
			}
		} catch (Exception e) {
			log.warn("消息广播异常：{}", e.getMessage());
		}
	}
	
	// 由于TICK数据可能太多，故作批量发送处理
	@Scheduled(cron="0/1 * * * * *")
	public void sendTicks() {
		List<String> data = cacheTicks.values().stream().map(tf -> Base64.encode(tf.toByteArray())).toList();
		cacheTicks.clear();
		emitterHolder.send(event().name(EventEmitterHolder.Event.TICKS.toString()).data(data));
	}

	/**
	 * 由于订单与成交信息在前端没有缓存手段，一刷新页面就会丢失，因此需要由后端缓存并定时推送
	 */
	public void resendCache() {
		cacheOrders.values().forEach(order -> {
			if(log.isDebugEnabled()) {
				log.debug("Order: {} {} {} {} {} {} {}", order.getOriginOrderId(), order.getOrderId(), order.getOrderDate(), order.getOrderTime(),
						order.getStatusMsg(), order.getDirection(), order.getOffsetFlag());
			}
			emitterHolder.send(event().name(NorthstarEventType.ORDER.toString()).data(Base64.encode(order.toByteArray())));
		});
		cacheTrades.values().forEach(trade -> {
			if(log.isDebugEnabled()) {
				log.debug("Trade: {} {} {} {} {} {} {}", trade.getOriginOrderId(), trade.getOrderId(), trade.getTradeDate(), trade.getTradeTime(),
						trade.getPrice(), trade.getDirection(), trade.getOffsetFlag());
			}
			emitterHolder.send(event().name(NorthstarEventType.TRADE.toString()).data(Base64.encode(trade.toByteArray())));
		});
	}
	
	public void resetCache() {
		cacheOrders.clear();
		cacheTrades.clear();
	}
	
	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return TARGET_TYPE.contains(eventType);
	}

	@Override
	protected void doHandle(NorthstarEvent e) {
		exec.execute(() -> doEmit(e));
	}
	
}
