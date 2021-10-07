package tech.xuanwu.northstar.main.restful;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ConcurrentMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.common.model.OrderRequest;
import tech.xuanwu.northstar.common.model.OrderRequest.TradeOperation;
import tech.xuanwu.northstar.domain.account.TradeDayAccount;
import tech.xuanwu.northstar.engine.broadcast.SocketIOMessageEngine;
import tech.xuanwu.northstar.engine.event.FastEventEngine;
import tech.xuanwu.northstar.main.NorthstarApplication;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = NorthstarApplication.class, value="spring.profiles.active=test")
public class TradeOperationTest {
	
	@MockBean
	ConcurrentMap<String, TradeDayAccount> accountMap;
	
	@Autowired
	TradeOperationController ctrlr;
	
	TradeDayAccount account;
	
	final String NAME = "test";
	
	@MockBean
	FastEventEngine feEngine;
	
	OrderRequest orderReq;
	
	@MockBean
	private SocketIOMessageEngine msgEngine;

	@Before
	public void setUp() throws Exception {
		account = mock(TradeDayAccount.class);
		when(accountMap.get(NAME)).thenReturn(account);
		
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void shouldOpenPosition() {
		orderReq = OrderRequest.builder()
				.gatewayId(NAME)
				.contractUnifiedSymbol("rb2210@SHFE@FUTURES")
				.tradeOpr(TradeOperation.BK)
				.price("1000")
				.volume(1)
				.build();
		ctrlr.submitOrder(orderReq);
		verify(account).openPosition(orderReq);
	}
	
	@Test
	public void shouldClosePosition() {
		orderReq = OrderRequest.builder()
				.gatewayId(NAME)
				.contractUnifiedSymbol("rb2210@SHFE@FUTURES")
				.tradeOpr(TradeOperation.BP)
				.price("1000")
				.volume(1)
				.build();
		ctrlr.submitOrder(orderReq);
		verify(account).closePosition(orderReq);
	}
	
	@Test
	public void shouldDispatchMessage() {
		ctrlr.tradeBySMS("this is mock");
		verify(feEngine).emitEvent(NorthstarEventType.EXT_MSG, "this is mock");
	}
}
