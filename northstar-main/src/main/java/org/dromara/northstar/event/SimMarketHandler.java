package org.dromara.northstar.event;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.account.AccountManager;
import org.dromara.northstar.account.GatewayManager;
import org.dromara.northstar.common.event.AbstractEventHandler;
import org.dromara.northstar.common.event.GenericEventHandler;
import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.gateway.MarketGateway;
import org.dromara.northstar.gateway.sim.trade.SimTradeGateway;

import xyz.redtorch.pb.CoreField.TickField;

public class SimMarketHandler extends AbstractEventHandler implements GenericEventHandler{

	private Executor exec = Executors.newSingleThreadExecutor();	// 增加一个工作线程解耦TICK事件可能导致的死锁问题
	
	private GatewayManager gatewayMgr;
	
	private AccountManager accountMgr;
	
	public SimMarketHandler(GatewayManager gatewayMgr, AccountManager accountMgr) {
		this.gatewayMgr = gatewayMgr;
		this.accountMgr = accountMgr;
	}
	
	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return NorthstarEventType.TICK == eventType;
	}

	@Override
	protected void doHandle(NorthstarEvent e) {
		if(NorthstarEventType.TICK == e.getEvent()) {			
			TickField tick = (TickField) e.getData();
			exec.execute(() -> 
				gatewayMgr.tradeGateways().stream()
					.filter(SimTradeGateway.class::isInstance)
					.map(SimTradeGateway.class::cast)
					.forEach(gw -> {
						MarketGateway mktGateway = accountMgr.get(Identifier.of(gw.gatewayId())).getMarketGateway();
						if(StringUtils.equals(mktGateway.gatewayId(), tick.getGatewayId())) {
							gw.onTick(tick);
						}
					})
			);
		}
	}

}
