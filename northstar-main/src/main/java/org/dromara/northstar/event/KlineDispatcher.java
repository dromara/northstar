package org.dromara.northstar.event;

import org.dromara.northstar.common.event.FastEventEngine.NorthstarEventDispatcher;
import org.dromara.northstar.common.model.Tuple;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.support.utils.EventEmitterHolder;

import cn.hutool.core.codec.Base64;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
public class KlineDispatcher implements NorthstarEventDispatcher{

	private EventEmitterHolder emitterHolder;
	
	private static final Set<Tuple<Contract, Interval>> registered = new HashSet<>();
	
	public KlineDispatcher(FastEventEngine feEngine, EventEmitterHolder emitterHolder) {
		feEngine.addHandler(this);
		this.emitterHolder = emitterHolder;
	}
	
	public synchronized void register(Contract contract, Interval interval) {
		registered.add(Tuple.of(contract, interval));
	}
	
	public synchronized void unregisterAll() {
		registered.clear();
	}
	
	@Override
	public void onEvent(NorthstarEvent event, long sequence, boolean endOfBatch) throws Exception {
		if(event.getData() instanceof Bar bar) {
			log.trace("Bar event received: {} {} {} {} unstable:{}", bar.contract().name(), bar.interval(), bar.actionDay(), bar.actionTime(), bar.unstable());
			Tuple<Contract, Interval> key = Tuple.of(bar.contract(), bar.interval());
			if(registered.contains(key)) {
				emitterHolder.send(SseEmitter.event()
						.name(NorthstarEventType.BAR.toString())
						.data(Base64.encode(bar.toBarField().toByteArray())));
			}
		}
	}

}
