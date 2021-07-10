package tech.xuanwu.northstar.strategy.common.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.gateway.api.TradeGateway;
import tech.xuanwu.northstar.strategy.common.constants.ModuleState;
import tech.xuanwu.northstar.strategy.common.event.EventDrivenComponent;
import tech.xuanwu.northstar.strategy.common.event.ModuleEvent;
import tech.xuanwu.northstar.strategy.common.event.ModuleEventBus;
import tech.xuanwu.northstar.strategy.common.event.ModuleEventType;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TradeField;

@Slf4j
@Builder
public class ModuleAgent implements EventDrivenComponent{
	
	/**
	 * 用于记录模组发出的订单
	 * originalOrderId --> order
	 */
	private final Map<String, OrderField> orderMap = new HashMap<>();
	/**
	 * 用于记录模组发出的订单的originOrderId
	 */
	private final Set<String> originOrderIdSet = new HashSet<>();
	/**
	 * 记录当天的成交
	 */
	private final Set<TradeField> currentDayTrade = new HashSet<>();
	
	private String name;

	private AccountField account;
	
	@Builder.Default
	private ModuleState state = ModuleState.EMPTY;
	
	private TradeGateway gateway;
	
	private String accountGatewayId;
	
	private boolean enabled;
	
	private String tradingDay;
	
	/**
	 * 更新账户
	 * @param account
	 */
	public void updateAccount(AccountField account) {
		this.account = account;
	}
	
	/**
	 * 更新交易日
	 * @param tradingDay
	 */
	public void updateTradingDay(String tradingDay) {
		if(!StringUtils.equals(this.tradingDay, tradingDay)) {
			this.tradingDay = tradingDay;
			currentDayTrade.clear();
			originOrderIdSet.clear();
			orderMap.clear();
		}
	}
	
	/**
	 * 获取交易日
	 * @return
	 */
	public String getTradingDay() {
		return null == tradingDay ? "" : tradingDay;
	}
	
	/**
	 * 获取账户网关ID
	 * @return
	 */
	public String getAccountGatewayId() {
		return accountGatewayId;
	}
	
	/**
	 * 获取模组状态
	 * @return
	 */
	public ModuleState getModuleState() {
		return state;
	}
	
	/**
	 * 获取模组名称
	 * @return
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * 获取账户余额
	 * @return
	 */
	public int getAccountBalance() {
		return (int) (account == null ? 0 : account.getBalance());
	}
	
	/**
	 * 获取账户可用余额
	 * @return
	 */
	public int getAccountAvailable() {
		return (int) (account == null ? 0 : account.getAvailable());
	}
	
	/**
	 * 切换模组状态
	 */
	public void toggleRunningState() {
		this.enabled = !enabled;
	}
	
	/**
	 * 是否启用
	 * @return
	 */
	public boolean isEnabled() {
		return enabled;
	}
	
	/**
	 * 是否有下单记录
	 * @param orderId
	 * @return
	 */
	public boolean hasOrderRecord(String originalOrderId) {
		return orderMap.containsKey(originalOrderId) || originOrderIdSet.contains(originalOrderId);
	}
	
	/**
	 * 获取当天开仓次数
	 * @return
	 */
	public long numOfOpeningForToday(){
		return currentDayTrade.stream()
				.filter(t -> t.getOffsetFlag() == OffsetFlagEnum.OF_Open)
				.count();
	}
	
	/**
	 * 更新订单
	 * @param order
	 */
	public void onOrder(OrderField order) {
		if(!originOrderIdSet.contains(order.getOriginOrderId())) {
			log.warn("该订单并不在模组的登记列表中，忽略不处理：{}", order);
			return;
		}
		if(order.getOrderStatus() == OrderStatusEnum.OS_Canceled
				|| order.getOrderStatus() == OrderStatusEnum.OS_Rejected) {
			orderMap.remove(order.getOriginOrderId());
			orderMap.remove(order.getOrderId());
			state = ModuleState.EMPTY;
		}else {
			orderMap.put(order.getOriginOrderId(), order);
			orderMap.put(order.getOrderId(), order);
			state = ModuleState.PENDING_ORDER;
		}
	}
	
	/**
	 * 更新订单
	 * @param trade
	 */
	public void onTrade(TradeField trade) {
		OrderField order = orderMap.remove(trade.getOriginOrderId());
		orderMap.remove(trade.getOrderId());
		if(order != null) {
			originOrderIdSet.remove(order.getOriginOrderId());
			currentDayTrade.add(trade);
			if(trade.getOffsetFlag() == OffsetFlagEnum.OF_Open) {
				state = ModuleState.HOLDING;
			} else {
				state = ModuleState.EMPTY;
			}
		}
	}
	
	@Override
	public void onEvent(ModuleEvent event) {
		if(ModuleEventType.ORDER_REQ_ACCEPTED == event.getEventType()) {
			SubmitOrderReqField orderReq = (SubmitOrderReqField) event.getData();
			if(!StringUtils.isNotBlank(orderReq.getOriginOrderId())) {
				state = ModuleState.EMPTY;
				throw new IllegalArgumentException("缺少必要的订单ID");
			}
			if(orderReq.getOffsetFlag() == OffsetFlagEnum.OF_Unkonwn) {
				state = ModuleState.EMPTY;
				throw new IllegalArgumentException("未定义开平仓类型：" + orderReq) ;
			}
			if(orderReq.getOffsetFlag() != OffsetFlagEnum.OF_Open && state != ModuleState.HOLDING) {
				state = ModuleState.EMPTY;
				log.info("没有对应的模组持仓，所以忽略平仓请求");
				return;
			}
			originOrderIdSet.add(orderReq.getOriginOrderId());
			gateway.submitOrder(orderReq);	
			state = ModuleState.PLACING_ORDER;	
			
		} else if(ModuleEventType.ORDER_REQ_CREATED == event.getEventType()) {
			state = ModuleState.PLACING_ORDER;	
			
		} else if(ModuleEventType.ORDER_RETRY == event.getEventType()) {
			SubmitOrderReqField orderReq = (SubmitOrderReqField) event.getData();
			originOrderIdSet.remove(orderReq.getOriginOrderId());
			CancelOrderReqField cancelReq = CancelOrderReqField.newBuilder()
					.setGatewayId(accountGatewayId)
					.setOriginOrderId(orderReq.getOriginOrderId())
					.build();
			gateway.cancelOrder(cancelReq);	
			state = ModuleState.TRACING_ORDER;
			
		} else if(ModuleEventType.ORDER_REQ_REJECTED == event.getEventType()) {
			state = ModuleState.EMPTY;
			
		}
	}

	@Override
	public void setEventBus(ModuleEventBus moduleEventBus) {
		// DO NOTHING
	}
}
