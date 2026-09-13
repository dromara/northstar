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
import org.dromara.northstar.common.model.Identifier;
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
		validateRunningTraderUpdate(lineTraderDTO);
		Double op = lineTraderDTO.getOrderPrice();
		Double sp = lineTraderDTO.getStopLossPrice();
		Double tp = lineTraderDTO.getTakeProfitPrice();
		String dir = lineTraderDTO.getPositionDir();
		// 仅对未入场的计划校验止盈止损与入场价的合理性，防止一创建就触发成交；
		// 运行中的计划已入场，止盈止损与入场价的相对关系不再有意义（如移动止损可越过入场价）
		if(Objects.nonNull(op) && lineTraderDTO.getState() == State.STANDBY
				&& ("long".equals(dir) && (tp < op || sp > op) || "short".equals(dir) && (sp < op || tp > op))) {
			throw new OperationException("error.incorrect_line_trader");
		}
		// 更新已有计划时复用原 trader，保留其内部委托状态（如未成交的入场委托），避免计划被误清理
		LineTrader trader = traderMap.get(lineTraderDTO.getId());
		if(Objects.nonNull(trader)) {
			trader.setData(lineTraderDTO);
		} else {
			trader = LineTrader.builder()
					.data(lineTraderDTO)
					.accountManager(accMgr)
					.marketCenter(mktCenter)
					.stateChangeCallback(stateChangeCallback)
					.build();
			traderMap.put(lineTraderDTO.getId(), trader);
		}
		emitterHolder.send(SseEmitter.event().name("LINE_TRADERS").data(traderMap.values().stream().map(LineTrader::getData).toList()));
		
		if(trader.hasPosition() && lineTraderDTO.getOrderPrice() == null) {
			lineTraderDTO.setState(State.RUNNING);
			stateChangeCallback.accept(lineTraderDTO);
		}
	}
	
	// 运行中的交易计划仅允许调整止盈止损价：原委托价在严格小于一个最小变动价位的误差内保持不变，
	// 方向、合约、账户、手数等关键字段一并冻结，防止同ID重保存篡改
	private void validateRunningTraderUpdate(LineTraderDTO lineTraderDTO) {
		LineTrader existingTrader = traderMap.get(lineTraderDTO.getId());
		if(Objects.isNull(existingTrader) || existingTrader.getData().getState() != State.RUNNING) {
			return;
		}
		LineTraderDTO original = existingTrader.getData();
		Double originalPrice = original.getOrderPrice();
		Double updatedPrice = lineTraderDTO.getOrderPrice();
		if(!Objects.equals(originalPrice, updatedPrice)) {
			if(Objects.isNull(originalPrice) || Objects.isNull(updatedPrice)) {
				throw new OperationException("error.unchangeable_line_trader_order_price");
			}
			double priceTick = mktCenter.getContract(Identifier.of(original.getContractId())).contract().priceTick();
			if(Math.abs(updatedPrice - originalPrice) >= priceTick) {
				throw new OperationException("error.unchangeable_line_trader_order_price");
			}
		}
		// 校验通过后统一冻结字段，状态由后端管理，防止请求方通过重设状态绕过运行中计划的校验
		lineTraderDTO.setState(State.RUNNING);
		lineTraderDTO.setOrderPrice(originalPrice);
		lineTraderDTO.setPositionDir(original.getPositionDir());
		lineTraderDTO.setContractId(original.getContractId());
		lineTraderDTO.setContractName(original.getContractName());
		lineTraderDTO.setAccountGatewayId(original.getAccountGatewayId());
		lineTraderDTO.setVolume(original.getVolume());
	}

	public synchronized List<LineTraderDTO> getTraders(){
		return traderMap.values().stream().map(LineTrader::getData).toList();
	}
	
	public boolean has(String id) {
		return traderMap.containsKey(id);
	}
	
	public synchronized LineTrader removeTrader(String id) {
		LineTrader removal = traderMap.remove(id);
		emitterHolder.send(SseEmitter.event().name("LINE_TRADERS").data(traderMap.values().stream().map(LineTrader::getData).toList()));
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
			if(trader.getData().getState() == State.COMPLETED) {
				itTrader.remove();
			}
		}
	}

}
