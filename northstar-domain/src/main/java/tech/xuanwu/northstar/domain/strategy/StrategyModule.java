package tech.xuanwu.northstar.domain.strategy;

import java.util.Set;
import java.util.function.Consumer;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.common.utils.FieldUtils;
import tech.xuanwu.northstar.strategy.api.EventDrivenComponent;
import tech.xuanwu.northstar.strategy.api.ModuleStatus;
import tech.xuanwu.northstar.strategy.api.ModuleStatusAware;
import tech.xuanwu.northstar.strategy.api.event.ModuleEvent;
import tech.xuanwu.northstar.strategy.api.event.ModuleEventBus;
import tech.xuanwu.northstar.strategy.api.event.ModuleEventType;
import tech.xuanwu.northstar.strategy.api.model.ModuleDealRecord;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 策略模组，作为策略组件的聚合根
 * @author KevinHuangwl
 *
 */
@Slf4j
public class StrategyModule implements EventDrivenComponent{
	
	protected ModuleEventBus meb = new ModuleEventBus();
	
	protected ModuleStatusHolder moduleStatus;
	
	protected ModuleStateMachine stateMachine;
	
	protected Set<EventDrivenComponent> components;
	
	@Getter
	@Setter
	protected boolean enabled;
	
	@Setter
	protected Consumer<SubmitOrderReqField> tradeIntentHandler;
	
	@Setter
	protected Consumer<ModuleStatusHolder> moduleStatusChangeHandler;
	
	@Setter
	protected Consumer<StrategyModule> runningStateChangeListener;
	
	@Setter
	protected Consumer<ModuleDealRecord> dealRecordGenHandler;
	
	private ModuleTradeIntent ti;
	
	public StrategyModule(ModuleStatus status) {
		this.moduleStatus = (ModuleStatusHolder) status;
		this.stateMachine = moduleStatus.getStateMachine();
	}
	
	/**
	 * 初始化组件
	 * @param component
	 */
	public void addComponent(EventDrivenComponent component) {
		components.add(component);
		if(component instanceof ModuleStatusAware) {
			ModuleStatusAware aware = (ModuleStatusAware) component;
			aware.setModuleStatus((ModuleStatus) moduleStatus);
		}
	}
	
	/**
	 * 模组名称
	 * @return
	 */
	public String getName() {
		return moduleStatus.getModuleName();
	}
	
	/**
	 * 切换模组启停
	 */
	public void toggleRunningState() {
		enabled = !enabled;
		if(runningStateChangeListener != null) {			
			runningStateChangeListener.accept(this);
		}
	}
	
	/**
	 * 全局事件分发
	 * @param event
	 */
	public void onEvent(NorthstarEvent event) {
		meb.post(event.getData());
		if(event.getEvent() == NorthstarEventType.TRADE && moduleStatusChangeHandler != null) {
			moduleStatusChangeHandler.accept(moduleStatus);
		}
		if(event.getEvent() == NorthstarEventType.TRADE && ti != null) {
			ti.onTrade((TradeField) event.getData());
		}
		if(event.getEvent() == NorthstarEventType.ORDER && ti != null) {
			ti.onOrder((OrderField) event.getData());
		}
	}

	/**
	 * 模组内部事件处理
	 */
	@Override
	public void onEvent(ModuleEvent<?> moduleEvent) {
		stateMachine.transformForm(moduleEvent.getEventType());
		if(moduleEvent.getEventType() == ModuleEventType.STOP_LOSS) {
			SubmitOrderReqField orderReq = (SubmitOrderReqField) moduleEvent.getData();
			log.info("[{}] 生成止损单{}：{}，{}，{}，{}手", getName(), orderReq.getOriginOrderId(), orderReq.getContract().getSymbol(),
					orderReq.getDirection(), orderReq.getOffsetFlag(), orderReq.getVolume());
			tradeIntentHandler.accept(orderReq);
			ti = genTradeIndent(orderReq);
		} else if(moduleEvent.getEventType() == ModuleEventType.ORDER_CONFIRMED) {
			SubmitOrderReqField orderReq = (SubmitOrderReqField) moduleEvent.getData();
			log.info("[{}] 生成订单{}：{}，{}，{}，{}手，价格{}，止损{}", getName(), orderReq.getOriginOrderId(), orderReq.getContract().getSymbol(),
					orderReq.getDirection(), orderReq.getOffsetFlag(), orderReq.getVolume(), orderReq.getPrice(), orderReq.getStopPrice());
			tradeIntentHandler.accept(orderReq);
			ti = genTradeIndent(orderReq);
		}
	}
	
	private ModuleTradeIntent genTradeIndent(SubmitOrderReqField submitOrder) {
		if(FieldUtils.isOpen(submitOrder.getOffsetFlag())) {
			return new ModuleTradeIntent(getName(), submitOrder, mpo -> mpo.ifPresent(mp -> moduleStatus.addPosition(mp)));
		}
		if(FieldUtils.isClose(submitOrder.getOffsetFlag())) {
			ModulePosition mp = null;
			if(FieldUtils.isBuy(submitOrder.getDirection())) {
				mp = moduleStatus.getShortPositions().get(submitOrder.getContract().getUnifiedSymbol());
			} 
			if(FieldUtils.isSell(submitOrder.getDirection())) {
				mp = moduleStatus.getLongPositions().get(submitOrder.getContract().getUnifiedSymbol());
			}
			if(mp == null) {
				throw new IllegalStateException("订单买卖方向不明确");
			}
			return new ModuleTradeIntent(getName(), mp, submitOrder, mdro -> mdro.ifPresent(dealRecordGenHandler));
		}
		throw new IllegalArgumentException("订单方向不明确");
	}

}
