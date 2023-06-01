package org.dromara.northstar.gateway.sim.trade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.dromara.northstar.gateway.sim.trade.OrderRequest.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreField.TickField;

class OrderReqManagerTest {

	private OrderReqManager manager;
	private OrderRequest mockOrderRequest;
	
	TestFieldFactory factory = new TestFieldFactory("testGateway");
	TickField mockTick = factory.makeTickField("rb2205@SHFE", 5111);

	@BeforeEach
	void setUp() {
		manager = new OrderReqManager();
		mockOrderRequest = Mockito.mock(OrderRequest.class);
	}

	@Test
	void testOnTick() {
	    when(mockOrderRequest.originOrderId()).thenReturn("order1");
	    manager.submitOrder(mockOrderRequest);
	    manager.onTick(mockTick);
	    verify(mockOrderRequest, times(1)).onTick(mockTick);
	}
	
	@Test
	void testSubmitOrder() {
	    when(mockOrderRequest.originOrderId()).thenReturn("order1");
	    manager.submitOrder(mockOrderRequest);
	    Map<String, OrderRequest> orderMap = manager.orderMap;
	    assertEquals(1, orderMap.size());
	    assertTrue(orderMap.containsKey("order1"));
	}
	
	@Test
	void testCancelOrder() {
	    when(mockOrderRequest.originOrderId()).thenReturn("order1");
	    manager.submitOrder(mockOrderRequest);
	    boolean result = manager.cancelOrder("order1");
	    assertTrue(result);
	    Map<String, OrderRequest> orderMap = manager.orderMap;
	    assertEquals(0, orderMap.size());
	}
	
	@Test
	void testTotalFrozenAmount() {
		when(mockOrderRequest.originOrderId()).thenReturn("1");
	    when(mockOrderRequest.orderType()).thenReturn(Type.OPEN);
	    when(mockOrderRequest.cost()).thenReturn(100.0);
	
	    OrderRequest mockOrderRequest2 = Mockito.mock(OrderRequest.class);
	    when(mockOrderRequest.originOrderId()).thenReturn("2");
	    when(mockOrderRequest2.orderType()).thenReturn(Type.OPEN);
	    when(mockOrderRequest2.cost()).thenReturn(200.0);
	
	    manager.submitOrder(mockOrderRequest);
	    manager.submitOrder(mockOrderRequest2);
	
	    double totalFrozenAmount = manager.totalFrozenAmount();
	    assertEquals(300.0, totalFrozenAmount, 0.0);
	}

}
