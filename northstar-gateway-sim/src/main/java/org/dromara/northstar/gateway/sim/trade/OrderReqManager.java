package org.dromara.northstar.gateway.sim.trade;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.gateway.sim.trade.OrderRequest.Type;

import xyz.redtorch.pb.CoreField.TickField;

/**
 * 委托请求管理器
 * @author KevinHuangwl
 *
 */
public class OrderReqManager implements TickDataAware{

	protected Map<String, OrderRequest> orderMap = new HashMap<>();
	
	@Override
	public void onTick(TickField tick) {
		orderMap.values().parallelStream().forEach(orderReq -> orderReq.onTick(tick));
	}
	
	public synchronized void submitOrder(OrderRequest orderReq) {
		orderMap.values().stream()
			.filter(OrderRequest::hasDone)
			.map(OrderRequest::originOrderId)
			.toList()
			.forEach(id -> orderMap.remove(id));
		
		orderMap.put(orderReq.originOrderId(), orderReq);
	}
	
	public synchronized boolean cancelOrder(String originOrderId) {
		return Objects.nonNull(orderMap.remove(originOrderId));
	}
	
	public synchronized double totalFrozenAmount() {
		return orderMap.values().stream()
					.filter(orderReq -> orderReq.orderType() == Type.OPEN)
					.mapToDouble(OrderRequest::cost)
					.sum();
	}
}
