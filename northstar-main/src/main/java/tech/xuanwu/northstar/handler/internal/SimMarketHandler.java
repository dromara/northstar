package tech.xuanwu.northstar.handler.internal;

import tech.xuanwu.northstar.common.event.AbstractEventHandler;
import tech.xuanwu.northstar.common.event.GenericEventHandler;
import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.gateway.sim.trade.SimMarket;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

public class SimMarketHandler extends AbstractEventHandler implements GenericEventHandler{

	private SimMarket market;
	
	public SimMarketHandler(SimMarket market) {
		this.market = market;
	}
	
	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return NorthstarEventType.TICK == eventType || NorthstarEventType.TRADE == eventType;
	}

	@Override
	protected void doHandle(NorthstarEvent e) {
		if(NorthstarEventType.TICK == e.getEvent()) {			
			TickField tick = (TickField) e.getData();
			market.onTick(tick);
		} else if(NorthstarEventType.TRADE == e.getEvent()) {
			TradeField trade = (TradeField) e.getData();
			market.onTrade(trade);
		}
	}

}
