package tech.xuanwu.northstar.manager;

import java.util.concurrent.ConcurrentHashMap;

import tech.xuanwu.northstar.common.event.AbstractEventHandler;
import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.strategy.common.constants.ModuleState;
import tech.xuanwu.northstar.strategy.common.model.ModulePerformance;
import tech.xuanwu.northstar.strategy.common.model.StrategyModule;
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
	
	private ConcurrentHashMap<String, StrategyModule> moduleMap = new ConcurrentHashMap<>(50);

	
	public void addModule(StrategyModule module) {
		moduleMap.put(module.getName(), module);
	}
	
	public void removeModule(String name) {
		if(moduleMap.get(name).getState() != ModuleState.EMPTY) {
			throw new IllegalStateException("模组并非处于空仓状态，不允许移除");
		}
		moduleMap.remove(name);
	}
	
	public ModulePerformance getModulePerformance(String name) {
		return moduleMap.get(name).getPerformance();
	}
	
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
