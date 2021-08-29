package tech.xuanwu.northstar.manager;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.event.AbstractEventHandler;
import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.persistence.ModuleRepository;
import tech.xuanwu.northstar.strategy.common.model.ModulePerformance;
import tech.xuanwu.northstar.strategy.common.model.StrategyModule;
import tech.xuanwu.northstar.strategy.common.model.entity.ModuleStatusEntity;
import tech.xuanwu.northstar.strategy.common.model.entity.TradeDescriptionEntity;
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
@Slf4j
public class ModuleManager extends AbstractEventHandler {
	/**
	 * moduleName --> module
	 */
	private ConcurrentHashMap<String, StrategyModule> moduleMap = new ConcurrentHashMap<>(50);
	
	private Set<NorthstarEventType> eventSet = new HashSet<>();
	
	private ModuleRepository moduleRepo;
	
	public ModuleManager(ModuleRepository moduleRepo) {
		this.moduleRepo = moduleRepo;
	}
	
	public void addModule(StrategyModule module) {
		moduleMap.put(module.getName(), module);
		eventSet.add(NorthstarEventType.ACCOUNT);
		eventSet.add(NorthstarEventType.TRADE);
		eventSet.add(NorthstarEventType.ORDER);
		eventSet.add(NorthstarEventType.TICK);
		eventSet.add(NorthstarEventType.IDX_TICK);
		eventSet.add(NorthstarEventType.BAR);
		eventSet.add(NorthstarEventType.EXT_MSG);
	}
	
	public StrategyModule removeModule(String name) {
		StrategyModule module = moduleMap.get(name);
		if(!module.removable()) {
			throw new IllegalStateException("模组并非处于空仓状态，不允许移除");
		}
		return moduleMap.remove(name);
	}
	
	public ModulePerformance getModulePerformance(String name) {
		return moduleMap.get(name).getPerformance();
	}
	
	public void toggleState(String name) {
		moduleMap.get(name).toggleRunningState();
	}
	
	public void onTick(TickField tick) {
		moduleMap.values().forEach(m -> m.onTick(tick));
	}
	
	public void onBar(BarField bar) {
		moduleMap.values().forEach(m -> m.onBar(bar));
	}
	
	public void onOrder(OrderField order) {
		moduleMap.values().forEach(m -> m.onOrder(order));
	}
	
	public void onTrade(TradeField trade) {
		// 只对持仓状态变化做持久化，不对下单状态作反应
		for(Entry<String, StrategyModule> e : moduleMap.entrySet()) {
			StrategyModule m = e.getValue();
			Optional<ModuleStatusEntity> result = m.onTrade(trade);
			if(result.isPresent()) {
				moduleRepo.saveTradeDescription(convertFrom(m.getName(), trade));
				moduleRepo.saveModuleStatus(result.get());
				return;
			}
		}
	}
	
	private TradeDescriptionEntity convertFrom(String moduleName, TradeField trade) {
		return TradeDescriptionEntity.builder()
				.moduleName(moduleName)
				.symbol(trade.getContract().getSymbol())
				.gatewayId(trade.getGatewayId())
				.direction(trade.getDirection())
				.offsetFlag(trade.getOffsetFlag())
				.contractMultiplier(trade.getContract().getMultiplier())
				.volume(trade.getVolume())
				.price(trade.getPrice())
				.tradingDay(trade.getTradingDay())
				.tradeTimestamp(trade.getTradeTimestamp())
				.build();
	}
	
	public void onAccount(AccountField account) {
		moduleMap.values().forEach(m -> m.onAccount(account));
	}
	
	private void onExtMsg(String message) {
		log.info("模组管理器分发外部信息");
		moduleMap.values().forEach(m -> m.onExternalMessage(message));
	}

	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return eventSet.contains(eventType);
	}

	@Override
	protected void doHandle(NorthstarEvent e) {
		switch(e.getEvent()){
		case TICK:
		case IDX_TICK:
			onTick((TickField) e.getData());
			break;
		case BAR:
			onBar((BarField) e.getData());
			break;
		case ORDER:
			onOrder((OrderField) e.getData());
			break;
		case TRADE:
			onTrade((TradeField) e.getData());
			break;
		case ACCOUNT:
			onAccount((AccountField) e.getData());
			break;
		case EXT_MSG:
			onExtMsg((String) e.getData());
			break;
		default:
			throw new IllegalStateException("未定义该事件-[" + e.getEvent() + "] 的处理方案");
		}
	}

}
