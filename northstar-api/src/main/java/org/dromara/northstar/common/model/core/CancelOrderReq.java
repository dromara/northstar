package org.dromara.northstar.common.model.core;

import lombok.Builder;

@Builder
public record CancelOrderReq(
		String gatewayId, 		// 网关ID
		String originOrderId, 	// 本地定义的订单ID
		String orderId			// 网关定义的订单ID
		) {

}
