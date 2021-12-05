package tech.quantit.northstar.domain.strategy;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.event.NorthstarEvent;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.utils.FieldUtils;
import tech.quantit.northstar.gateway.api.TradeGateway;
import tech.quantit.northstar.strategy.api.ContractBindedAware;
import tech.quantit.northstar.strategy.api.EventDrivenComponent;
import tech.quantit.northstar.strategy.api.SignalPolicy;
import tech.quantit.northstar.strategy.api.StateChangeListener;
import tech.quantit.northstar.strategy.api.event.ModuleEvent;
import tech.quantit.northstar.strategy.api.event.ModuleEventBus;
import tech.quantit.northstar.strategy.api.event.ModuleEventType;
import tech.quantit.northstar.strategy.api.model.ModuleDealRecord;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 策略模组，作为策略组件的聚合根
 * @author KevinHuangwl
 *
 */
@Slf4j
public class StrategyModule implements EventDrivenComponent{
	
	protected ModuleEventBus meb = new ModuleEventBus();
	
	@Getter
	protected ModuleStatus moduleStatus;
	
	protected ModuleStateMachine stateMachine;
	
	protected Set<EventDrivenComponent> components = new HashSet<>();
	
	@Getter
	@Setter
	protected boolean enabled;
	
	@Setter
	protected Consumer<SubmitOrderReqField> submitOrderHandler;
	
	@Setter
	protected Consumer<CancelOrderReqField> cancelOrderHandler;
	
	@Setter
	protected Consumer<ModuleStatus> moduleStatusChangeHandler;
	
	@Setter
	protected Consumer<Boolean> runningStateChangeListener;
	
	@Setter
	protected Consumer<ModuleDealRecord> dealRecordGenHandler;
	
	protected ModuleTradeIntent ti;
	
	@Getter
	private String bindedMktGatewayId;
	@Getter
	private TradeGateway gateway;
	@Getter
	private SignalPolicy signalPolicy;
	
	private Set<String> bindedSymbols = new HashSet<>();
	
	public StrategyModule(String bindedMktGatewayId, TradeGateway gateway, ModuleStatus status) {
		this.moduleStatus = status;
		this.moduleStatus.setModuleEventBus(meb);
		this.meb.register(status);
		this.stateMachine = moduleStatus.getStateMachine();
		this.bindedMktGatewayId = bindedMktGatewayId;
		this.gateway = gateway;
	}
	
	/**
	 * 初始化组件
	 * @param component
	 */
	public void addComponent(EventDrivenComponent component) {
		components.add(component);
		meb.register(component);
		component.setEventBus(meb);
		if(component instanceof StateChangeListener listener) {			
			moduleStatus.getStateMachine().addStateChangeListener(listener);
		}
		if(component instanceof SignalPolicy policy) {
			signalPolicy = policy;
		}
		if(component instanceof ContractBindedAware aware) {
			bindedSymbols.add(aware.bindedContractSymbol());
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
			runningStateChangeListener.accept(isEnabled());
		}
	}
	
	/**
	 * 全局事件分发
	 * @param event
	 */
	public void onEvent(NorthstarEvent event) {
		meb.post(event.getData());
		if(event.getEvent() == NorthstarEventType.TRADE) 
			handleTrade((TradeField) event.getData());
		if(event.getEvent() == NorthstarEventType.ORDER) 
			handleOrder((OrderField) event.getData());
	}
	
	/**
	 * 过滤多余事件
	 * @param event
	 * @return
	 */
	public boolean canHandle(NorthstarEvent event) {
		if(event.getEvent() == NorthstarEventType.TICK || event.getEvent() == NorthstarEventType.IDX_TICK)
			return bindedSymbols.contains(((TickField)event.getData()).getUnifiedSymbol());
		if(event.getEvent() == NorthstarEventType.BAR)
			return bindedSymbols.contains(((BarField)event.getData()).getUnifiedSymbol());
		if(event.getEvent() == NorthstarEventType.ORDER) 
			return ti != null && ((OrderField)event.getData()).getOriginOrderId().equals(ti.originOrderId());
		if(event.getEvent() == NorthstarEventType.TRADE) 
			return ti != null && ((TradeField)event.getData()).getOriginOrderId().equals(ti.originOrderId());
		if(event.getEvent() == NorthstarEventType.ACCOUNT)
			return ((AccountField)event.getData()).getGatewayId().equals(gateway.getGatewaySetting().getGatewayId());
		return true;
	}
	
	private void handleOrder(OrderField order) {
		if(order.getOrderStatus() == OrderStatusEnum.OS_Canceled) {			
			moduleStatus.getStateMachine().transformForm(ModuleEventType.ORDER_CANCELLED);
			meb.post(new ModuleEvent<>(ModuleEventType.ORDER_CANCELLED, order));
		}
		if(order.getOrderStatus() == OrderStatusEnum.OS_Unknown || order.getOrderStatus() == OrderStatusEnum.OS_Touched)
			moduleStatus.getStateMachine().transformForm(ModuleEventType.ORDER_CONFIRMED);
		if(ti != null)
			ti.onOrder(order);
	}
	
	private void handleTrade(TradeField trade) {
		if(FieldUtils.isBuy(trade.getDirection()))
			moduleStatus.getStateMachine().transformForm(ModuleEventType.BUY_TRADED);
		if(FieldUtils.isSell(trade.getDirection()))
			moduleStatus.getStateMachine().transformForm(ModuleEventType.SELL_TRADED);
		if(moduleStatusChangeHandler != null)	
			moduleStatusChangeHandler.accept(moduleStatus);
		if(ti != null)	
			ti.onTrade(trade);
	}
	
	/**
	 * 获取绑定合约
	 * @return
	 */
	public Set<String> bindedContractUnifiedSymbols(){
		return bindedSymbols;
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
			submitOrderHandler.accept(orderReq);
			ti = genTradeIndent(orderReq);
		} else if(moduleEvent.getEventType() == ModuleEventType.ORDER_CONFIRMED) {
			SubmitOrderReqField orderReq = (SubmitOrderReqField) moduleEvent.getData();
			log.info("[{}] 生成订单{}：{}，{}，{}，{}手，价格{}，止损{}", getName(), orderReq.getOriginOrderId(), orderReq.getContract().getSymbol(),
					orderReq.getDirection(), orderReq.getOffsetFlag(), orderReq.getVolume(), orderReq.getPrice(), orderReq.getStopPrice());
			submitOrderHandler.accept(orderReq);
			ti = genTradeIndent(orderReq);
		} else if(moduleEvent.getEventType() == ModuleEventType.ORDER_REQ_CANCELLED) {
			CancelOrderReqField cancelOrderReq = (CancelOrderReqField) moduleEvent.getData();
			log.info("[{}] 撤单：{}", getName(), cancelOrderReq.getOriginOrderId());
			cancelOrderHandler.accept(cancelOrderReq);
		}
	}
	
	private ModuleTradeIntent genTradeIndent(SubmitOrderReqField submitOrder) {
		if(FieldUtils.isOpen(submitOrder.getOffsetFlag())) {
			return new ModuleTradeIntent(getName(), submitOrder, mpo -> mpo.ifPresent(mp -> {
				mp.setClearoutCallback(p -> moduleStatus.removePostion(p));
				moduleStatus.addPosition(mp);
				meb.register(mp);
			}));
		}
		if(FieldUtils.isClose(submitOrder.getOffsetFlag())) {
			ModulePosition mp = null;
			if(FieldUtils.isBuy(submitOrder.getDirection())) {
				mp = moduleStatus.getShortPosition();
			} 
			if(FieldUtils.isSell(submitOrder.getDirection())) {
				mp = moduleStatus.getLongPosition();
			}
			if(mp == null) {
				throw new IllegalStateException("没有持仓信息");
			}
			return new ModuleTradeIntent(getName(), mp, submitOrder, mdro -> mdro.ifPresent(dealRecordGenHandler));
		}
		throw new IllegalArgumentException("订单方向不明确");
	}

	@Override
	public void setEventBus(ModuleEventBus moduleEventBus) {
		throw new UnsupportedOperationException("该方法不支持");
	}

}
