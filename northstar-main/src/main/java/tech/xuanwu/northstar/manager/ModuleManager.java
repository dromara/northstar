package tech.xuanwu.northstar.manager;

import tech.xuanwu.northstar.common.event.AbstractEventHandler;
import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 用于管理与缓存模组对象
 * @author KevinHuangwl
 *
 */
public class ModuleManager extends AbstractEventHandler {

	
	public void addModule() {}
	
	public void removeModule() {}
	
	public void onTick(TickField tick) {
		
	}
	
	public void onBar(BarField bar) {
		
	}
	
	public void onOrder(OrderField order) {
		
	}
	
	public void onTrade(TradeField trade) {
		
	}
	
	public void onAccount(AccountField account) {
		
	}

	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void doHandle(NorthstarEvent e) {
		// TODO Auto-generated method stub
		
	}
	
	
}
