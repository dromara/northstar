package tech.xuanwu.northstar.handler.internal;

import tech.xuanwu.northstar.common.event.AbstractEventHandler;
import tech.xuanwu.northstar.common.event.GenericEventHandler;
import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.gateway.sim.SimMarket;
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
		TickField tick = (TickField) e.getData();
		market.update(tick);
	}

}
