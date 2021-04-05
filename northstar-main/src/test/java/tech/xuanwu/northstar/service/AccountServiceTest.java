package tech.xuanwu.northstar.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Before;
import org.junit.Test;

import tech.xuanwu.northstar.common.event.InternalEventBus;
import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.common.exception.InsufficientException;
import tech.xuanwu.northstar.common.exception.NoSuchElementException;
import tech.xuanwu.northstar.common.exception.TradeException;
import tech.xuanwu.northstar.common.model.GatewayDescription;
import tech.xuanwu.northstar.common.model.OrderRecall;
import tech.xuanwu.northstar.common.model.OrderRequest;
import tech.xuanwu.northstar.common.model.OrderRequest.TradeOperation;
import tech.xuanwu.northstar.domain.GatewayConnection;
import tech.xuanwu.northstar.domain.TradeDayAccount;

public class AccountServiceTest {
	
	AccountService service = new AccountService();
	
	@Before
	public void prepare() {
		service.eventBus = mock(InternalEventBus.class);
		
		service.gatewayContractMap = mock(ConcurrentHashMap.class);
	}

	@Test
	public void testSubmitOrder() throws InsufficientException {
		service.accountMap = mock(ConcurrentHashMap.class);
		TradeDayAccount account = mock(TradeDayAccount.class);
		when(service.accountMap.get("testGateway")).thenReturn(account);
		
		OrderRequest req1 = new OrderRequest("rb2102", "1234", null, 1, TradeOperation.BK, "testGateway");
		service.submitOrder(req1);
		verify(account).openPosition(req1);
		
		OrderRequest req2 = new OrderRequest("rb2102", "1234", null, 1, TradeOperation.BP, "testGateway");
		service.submitOrder(req2);
		verify(account).closePosition(req2);
	}
	
	@Test(expected = NoSuchElementException.class)
	public void testSubmitOrderWithException() throws InsufficientException {
		service.accountMap = mock(ConcurrentHashMap.class);
		OrderRequest req = mock(OrderRequest.class);
		service.submitOrder(req);
	}

	@Test
	public void testCancelOrder() throws TradeException {
		service.accountMap = mock(ConcurrentHashMap.class);
		TradeDayAccount account = mock(TradeDayAccount.class);
		when(service.accountMap.get("testGateway")).thenReturn(account);
		
		OrderRecall recall = new OrderRecall("123456", "testGateway");
		service.cancelOrder(recall);
		
		verify(account).cancelOrder(recall);
	}
	
	@Test(expected = NoSuchElementException.class)
	public void testCancelOrderWithException() throws TradeException {
		service.cancelOrder(new OrderRecall("123456", "testGateway"));
	}

	@Test
	public void testOnLogined() {
		service.gatewayContractMap = mock(Map.class);
		when(service.gatewayContractMap.containsKey("testGateway")).thenReturn(true);
		when(service.gatewayContractMap.get("testGateway")).thenReturn(mock(Map.class));
		service.onLogined(new NorthstarEvent(NorthstarEventType.LOGINED, "testGateway"));
	}
	
	@Test(expected = NoSuchElementException.class)
	public void testOnLoginedWithException() {
		service.onLogined(new NorthstarEvent(NorthstarEventType.LOGINED, "testGateway"));
	}

	@Test
	public void testOnLogouted() {
		service.gatewayContractMap = mock(Map.class);
		GatewayConnection conn = mock(GatewayConnection.class);
		when(conn.getGwDescription()).thenReturn(mock(GatewayDescription.class));
		when(conn.getGwDescription().getGatewayId()).thenReturn("123456");
		
		service.onLogouted(new NorthstarEvent(NorthstarEventType.DISCONNECTING, conn));
	}

}
