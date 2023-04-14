package tech.quantit.northstar.main.restful;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ConcurrentMap;

import org.dromara.northstar.NorthstarApplication;
import org.dromara.northstar.domain.account.TradeDayAccount;
import org.dromara.northstar.main.handler.broadcast.SocketIOMessageEngine;
import org.dromara.northstar.main.restful.TradeOperationController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.corundumstudio.socketio.SocketIOServer;

import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.model.OrderRequest;
import tech.quantit.northstar.common.model.OrderRequest.TradeOperation;

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
	
	@MockBean
	private SocketIOServer socketServer;
	
	@BeforeEach
	public void setUp() throws Exception {
		account = mock(TradeDayAccount.class);
		when(accountMap.get(NAME)).thenReturn(account);
		
	}

	@AfterEach
	public void tearDown() throws Exception {
	}

	@Test
	public void shouldOpenPosition() {
		orderReq = OrderRequest.builder()
				.gatewayId(NAME)
				.contractId("rb2210@SHFE@FUTURES")
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
				.contractId("rb2210@SHFE@FUTURES")
				.tradeOpr(TradeOperation.BP)
				.price("1000")
				.volume(1)
				.build();
		ctrlr.submitOrder(orderReq);
		verify(account).closePosition(orderReq);
	}
	
}
