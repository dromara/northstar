package org.dromara.northstar.main.handler.internal;

import org.dromara.northstar.common.event.AbstractEventHandler;
import org.dromara.northstar.common.event.GenericEventHandler;
import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.gateway.sim.trade.SimMarket;

import xyz.redtorch.pb.CoreField.TickField;

public class SimMarketHandler extends AbstractEventHandler implements GenericEventHandler{

	private SimMarket market;
	
	public SimMarketHandler(SimMarket market) {
		this.market = market;
	}
	
	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return NorthstarEventType.TICK == eventType;
	}

	@Override
	protected void doHandle(NorthstarEvent e) {
		if(NorthstarEventType.TICK == e.getEvent()) {			
			TickField tick = (TickField) e.getData();
			market.onTick(tick);
		}
	}

}
