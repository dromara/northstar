package tech.quantit.northstar.domain.strategy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import lombok.Getter;
import lombok.Setter;
import tech.quantit.northstar.common.ContractBindedAware;
import tech.quantit.northstar.common.event.NorthstarEvent;
import tech.quantit.northstar.common.utils.BarUtils;
import tech.quantit.northstar.gateway.api.TradeGateway;
import tech.quantit.northstar.strategy.api.EventDrivenComponent;
import tech.quantit.northstar.strategy.api.SignalPolicy;
import tech.quantit.northstar.strategy.api.StateChangeListener;
import tech.quantit.northstar.strategy.api.event.ModuleEvent;
import tech.quantit.northstar.strategy.api.event.ModuleEventBus;
import tech.quantit.northstar.strategy.api.event.ModuleEventType;
import tech.quantit.northstar.strategy.api.log.NorthstarLoggerFactory;
import tech.quantit.northstar.strategy.api.model.ModuleDealRecord;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 策略模组，作为策略组件的聚合根
 * @author KevinHuangwl
 *
 */
public class StrategyModule implements EventDrivenComponent{
	
	protected ModuleEventBus meb = new ModuleEventBus();
	
	@Getter
	protected ModuleStatus moduleStatus;
	
	protected ModuleStateMachine stateMachine;
	
	protected Set<EventDrivenComponent> components = new HashSet<>();
	
	@Getter
	protected boolean enabled;
	
	@Setter
	protected Consumer<SubmitOrderReqField> submitOrderHandler;
	
	@Setter
	protected Consumer<CancelOrderReqField> cancelOrderHandler;
	
	@Setter
	protected Consumer<Boolean> runningStateChangeListener;
	
	@Setter
	protected Consumer<ModuleDealRecord> dealRecordGenHandler;
	
	@Setter
	protected Consumer<TradeField> savingTradeCallback;
	
	protected ModuleTradeIntent ti;
	
	@Getter
	private String bindedMktGatewayId;
	@Getter
	private TradeGateway gateway;
	@Getter
	private SignalPolicy signalPolicy;
	
	private Set<String> bindedSymbols = new HashSet<>();
	
	private List<BarField> periodBars;
	
	private Logger log;
	
	public StrategyModule(String bindedMktGatewayId, TradeGateway gateway, ModuleStatus status) {
		this.moduleStatus = status;
		this.moduleStatus.setModuleEventBus(meb);
		this.stateMachine = moduleStatus.getStateMachine();
		this.bindedMktGatewayId = bindedMktGatewayId;
		this.gateway = gateway;
		this.meb.register(status);
		this.meb.register(this);
		this.log = NorthstarLoggerFactory.getLogger(status.getModuleName(), getClass());
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
			periodBars = new ArrayList<>(signalPolicy.periodMins());
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
		meb.post(new ModuleEvent<>(ModuleEventType.MODULE_TOGGLE, enabled));
	}
	
	/**
	 * 全局事件分发
	 * @param event
	 */
	public void onEvent(NorthstarEvent event) {
		if(event.getData() instanceof BarField bar) {
			if(!StringUtils.equals(bar.getUnifiedSymbol(), signalPolicy.bindedContractSymbol())) {
				return;
			}
			periodBars.add(bar);
			if(periodBars.size() >= signalPolicy.periodMins()) {
				meb.post(BarUtils.merge(periodBars));
				periodBars.clear();
			}
		} else {			
			meb.post(event.getData());
		}
		if(ti != null && event.getData() instanceof TradeField trade && trade.getOriginOrderId().equals(ti.getSubmitOrderReq().getOriginOrderId())) {			
			SubmitOrderReqField orderReq = ti.getSubmitOrderReq();
			log.debug("[{}] 收到成交回报，订单号:{}", moduleStatus.getModuleName(), trade.getOriginOrderId());
			ti.onTrade(trade);
			moduleStatus.updatePosition(trade, orderReq);
			savingTradeCallback.accept(trade);
		} 
		if(ti !=null && event.getData() instanceof OrderField order && order.getOriginOrderId().equals(ti.getSubmitOrderReq().getOriginOrderId())) {			
			log.debug("[{}] 收到订单回报，订单号：{}，订单状态{}", moduleStatus.getModuleName(), order.getOriginOrderId(), order.getOrderStatus());
			if(order.getOrderStatus() == OrderStatusEnum.OS_Canceled || order.getOrderStatus() == OrderStatusEnum.OS_Rejected) {
				meb.post(new ModuleEvent<>(ModuleEventType.ORDER_CANCELLED, order));
			}else {			
				meb.post(new ModuleEvent<>(ModuleEventType.ORDER_CONFIRMED, order));
			}
			ti.onOrder(order);
		}
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
			ti = (ModuleTradeIntent) moduleEvent.getData();
			SubmitOrderReqField orderReq = ti.getSubmitOrderReq();
			log.info("[{}] 生成止损单{}：{}，{}，{}，{}手", getName(), orderReq.getOriginOrderId(), orderReq.getContract().getSymbol(),
					orderReq.getDirection(), orderReq.getOffsetFlag(), orderReq.getVolume());
			submitOrderHandler.accept(orderReq);
		} else if(moduleEvent.getEventType() == ModuleEventType.ORDER_REQ_ACCEPTED) {
			SubmitOrderReqField orderReq;
			if(moduleEvent.getData() instanceof ModuleTradeIntent mti) {
				ti = mti;
				orderReq = mti.getSubmitOrderReq();
			} else {
				orderReq = (SubmitOrderReqField) moduleEvent.getData();				
				ti = new ModuleTradeIntent(getName(), orderReq, dealRecordGenHandler, () -> ti = null);
			}
			log.info("[{}] 生成订单{}：{}，{}，{}，{}手，价格{}，止损{}", getName(), orderReq.getOriginOrderId(), orderReq.getContract().getSymbol(),
					orderReq.getDirection(), orderReq.getOffsetFlag(), orderReq.getVolume(), orderReq.getPrice(), orderReq.getStopPrice());
			submitOrderHandler.accept(orderReq);
		} else if(moduleEvent.getEventType() == ModuleEventType.ORDER_REQ_CANCELLED) {
			CancelOrderReqField cancelOrderReq = (CancelOrderReqField) moduleEvent.getData();
			log.info("[{}] 撤单：{}", getName(), cancelOrderReq.getOriginOrderId());
			cancelOrderHandler.accept(cancelOrderReq);
		}
	}
	
	@Override
	public void setEventBus(ModuleEventBus moduleEventBus) {
		throw new UnsupportedOperationException("该方法不支持");
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		meb.post(new ModuleEvent<>(ModuleEventType.MODULE_TOGGLE, enabled));
	}

}
