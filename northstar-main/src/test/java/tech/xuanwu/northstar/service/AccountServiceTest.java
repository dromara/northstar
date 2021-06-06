package tech.xuanwu.northstar.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpSession;

import org.junit.Before;
import org.junit.Test;

import tech.xuanwu.northstar.common.exception.InsufficientException;
import tech.xuanwu.northstar.common.exception.NoSuchElementException;
import tech.xuanwu.northstar.common.exception.TradeException;
import tech.xuanwu.northstar.common.model.OrderRecall;
import tech.xuanwu.northstar.common.model.OrderRequest;
import tech.xuanwu.northstar.common.model.OrderRequest.TradeOperation;
import tech.xuanwu.northstar.domain.account.TradeDayAccount;

public class AccountServiceTest {
	
	AccountService service;
	
	@Before
	public void prepare() {
		service = new AccountService(mock(ConcurrentHashMap.class));
		HttpSession session = mock(HttpSession.class);
	}
	

	@Test
	public void testSubmitOrder() throws InsufficientException {
		
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

}
