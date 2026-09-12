package org.dromara.northstar.event;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import org.dromara.northstar.account.AccountManager;
import org.dromara.northstar.common.event.AbstractEventHandler;
import org.dromara.northstar.common.event.GenericEventHandler;
import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.exception.OperationException;
import org.dromara.northstar.common.model.LineTraderDTO;
import org.dromara.northstar.common.model.LineTraderDTO.State;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.dromara.northstar.support.utils.EventEmitterHolder;
import org.dromara.northstar.trader.LineTrader;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PlannedTradeHandler extends AbstractEventHandler implements GenericEventHandler{
	
	private AccountManager accMgr;
	
	private MarketCenter mktCenter;
	
	private EventEmitterHolder emitterHolder;
	
	private ExecutorService exec;
	
	private Map<String, LineTrader> traderMap = new HashMap<>();
	
	private static final Set<NorthstarEventType> TARGET_TYPE = EnumSet.of(
			NorthstarEventType.TICK, 
			NorthstarEventType.ORDER,
			NorthstarEventType.TRADE
	);
	
	private Consumer<LineTraderDTO> stateChangeCallback = dto -> {
		log.info("画线交易计划[{}] 状态变更：{}", dto.getId(), dto.getState());
		exec.execute(() -> {
			emitterHolder.send(SseEmitter.event().name("LINE_TRADER_" + dto.getState()).data(dto.getId()));
		});
	};
	
	public PlannedTradeHandler(AccountManager accMgr, MarketCenter mktCenter, EventEmitterHolder emitterHolder) {
		this.accMgr = accMgr;
		this.mktCenter = mktCenter;
		this.emitterHolder = emitterHolder;
		this.exec = CommonUtils.newThreadPerTaskExecutor(getClass());
	}
	
	public synchronized void addTrader(LineTraderDTO lineTraderDTO) {
		Double op = lineTraderDTO.getOrderPrice();
		Double sp = lineTraderDTO.getStopLossPrice();
		Double tp = lineTraderDTO.getTakeProfitPrice();
		String dir = lineTraderDTO.getPositionDir();
		if(Objects.nonNull(op) && ("long".equals(dir) && (tp < op || sp > op) || "short".equals(dir) && (sp < op || tp > op))) {
			throw new OperationException("error.incorrect_line_trader");
		}
		LineTrader trader = LineTrader.builder()
				.dto(lineTraderDTO)
				.accountManager(accMgr)
				.marketCenter(mktCenter)
				.stateChangeCallback(stateChangeCallback)
				.build();
		traderMap.put(lineTraderDTO.getId(), trader);
		emitterHolder.send(SseEmitter.event().name("LINE_TRADERS").data(traderMap.values().stream().map(LineTrader::getDto).toList()));
		
		if(trader.hasPosition() && lineTraderDTO.getOrderPrice() == null) {
			lineTraderDTO.setState(State.RUNNING);
			stateChangeCallback.accept(lineTraderDTO);
		}
	}
	
	public synchronized List<LineTraderDTO> getTraders(){
		return traderMap.values().stream().map(LineTrader::getDto).toList();
	}
	
	public boolean has(String id) {
		return traderMap.containsKey(id);
	}
	
	public synchronized LineTrader removeTrader(String id) {
		LineTrader removal = traderMap.remove(id);
		emitterHolder.send(SseEmitter.event().name("LINE_TRADERS").data(traderMap.values().stream().map(LineTrader::getDto).toList()));
		return removal;
	}

	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return TARGET_TYPE.contains(eventType);
	}

	@Override
	protected synchronized void doHandle(NorthstarEvent e) {
		if(e.getData() instanceof Trade t) {
			traderMap.values().forEach(trader -> trader.onTrade(t));
		}
		if(e.getData() instanceof Order o) {
			traderMap.values().forEach(trader -> trader.onOrder(o));
		}
		if(e.getData() instanceof Tick t) {
			try {				
				traderMap.values().forEach(trader -> trader.onTick(t));
			} catch (Exception ex) {
				log.warn("无法进行交易计划，原因：" + ex.getLocalizedMessage());
				emitterHolder.send(SseEmitter.event().name("COMS_WARN").data(ex.getLocalizedMessage()));
				return;
			}
		}
		Iterator<LineTrader> itTrader = traderMap.values().iterator();
		while(itTrader.hasNext()) {
			LineTrader trader = itTrader.next();
			if(trader.getDto().getState() == State.COMPLETED) {
				itTrader.remove();
			}
		}
	}

}
