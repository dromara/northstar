package tech.quantit.northstar.main.handler.internal;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.event.AbstractEventHandler;
import tech.quantit.northstar.common.event.NorthstarEvent;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.gateway.api.domain.LatencyDetector;
import tech.quantit.northstar.gateway.sim.trade.SimMarket;
import tech.quantit.northstar.strategy.api.IModule;
import xyz.redtorch.pb.CoreField.TickField;

@Slf4j
public class ModuleManager extends AbstractEventHandler{
	/**
	 * moduleName --> module
	 */
	protected ConcurrentHashMap<String, IModule> moduleMap = new ConcurrentHashMap<>(50);
	
	private SimMarket simMarket;
	
	private static final Set<NorthstarEventType> TARGET_TYPE = EnumSet.of(
			NorthstarEventType.ACCOUNT,
			NorthstarEventType.TRADE,
			NorthstarEventType.ORDER,
			NorthstarEventType.TICK,
			NorthstarEventType.BAR
	);
	
	private LatencyDetector latencyDetector;
	
	public ModuleManager(SimMarket simMarket, LatencyDetector latencyDetector) {
		this.latencyDetector = latencyDetector;
		this.simMarket = simMarket;
	}
	
	public void addModule(IModule module) {
		moduleMap.put(module.getName(), module);
	}
	
	public void removeModule(String name) {
		IModule module = moduleMap.get(name);
		if(module == null) {
			log.debug("[{}] 已删除", name);
			return;
		} 	
		if(module.isEnabled()) {
			throw new IllegalStateException("模组处于启用状态，不允许移除");
		}
		moduleMap.remove(name);
	}
	
	public IModule getModule(String name) {
		if(!moduleMap.containsKey(name)) {
			throw new IllegalStateException("没有找到模组：" + name);
		}
		return moduleMap.get(name);
	}
	

	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return TARGET_TYPE.contains(eventType);
	}

	@Override
	protected void doHandle(NorthstarEvent e) {
		if(latencyDetector != null && e.getData() instanceof TickField tick) {
			// 分发到模组前的检测点
			latencyDetector.getCheckpoint(0).sampling(tick);
		}
		moduleMap.values().parallelStream().forEach(sm -> sm.onEvent(e));
		
		if(e.getData() instanceof TickField tick) {
			simMarket.onTick(tick);
		}
	}
}
