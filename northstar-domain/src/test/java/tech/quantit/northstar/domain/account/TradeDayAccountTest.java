package tech.quantit.northstar.domain.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import tech.quantit.northstar.common.event.InternalEventBus;
import tech.quantit.northstar.common.event.NorthstarEvent;
import tech.quantit.northstar.common.exception.InsufficientException;
import tech.quantit.northstar.common.exception.TradeException;
import tech.quantit.northstar.common.model.ContractManager;
import tech.quantit.northstar.common.model.OrderRecall;
import tech.quantit.northstar.common.model.OrderRequest;
import tech.quantit.northstar.common.model.OrderRequest.TradeOperation;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.TradeField;

public class TradeDayAccountTest {
	
	TradeDayAccount tda;
	
	ContractField contract = ContractField.newBuilder()
			.setContractId("rb2102@SHFE")
			.setExchange(ExchangeEnum.SHFE)
			.setGatewayId("testGateway")
			.setSymbol("rb2102")
			.setUnifiedSymbol("rb2102@SHFE")
			.setMultiplier(10)
			.setLongMarginRatio(0.08)
			.setShortMarginRatio(0.08)
			.build();
	
	@BeforeEach
	public void prepare() {
		InternalEventBus eventBus = mock(InternalEventBus.class);
		ContractManager contractMgr = mock(ContractManager.class);
		when(contractMgr.getContract("rb2102@SHFE")).thenReturn(contract);
		tda = new TradeDayAccount("testGateway", eventBus, contractMgr);
	}
	

	@Test
	public void testOnAccountUpdate() {
		AccountField af = AccountField.newBuilder()
				.setAccountId("testAccount")
				.setBalance(10022)
				.setAvailable(5000)
				.build();
		tda.onAccountUpdate(af);
		
		assertThat(tda.getAccountInfo().getBalance()).isCloseTo(10022, offset(0.00001));
		assertThat(tda.getAccountInfo().getAccountId()).isEqualTo("testAccount");
	}

	@Test
	public void testOnPositionUpdate() {
		PositionField pf = PositionField.newBuilder()
				.setPositionId("123456")
				.setPositionDirection(PositionDirectionEnum.PD_Long)
				.setPosition(2)
				.setContract(contract)
				.setTdPosition(1)
				.setYdPosition(1)
				.build();
		tda.onPositionUpdate(pf);
		
		assertThat(tda.getPositions().size()).isEqualTo(1);
	}

	@Test
	public void testOnTradeUpdate() {
		TradeField tf = TradeField.newBuilder()
				.setTradeId("456789")
				.build();
		tda.onTradeUpdate(tf);
		assertThat(tda.getTradeDayTransactions().size()).isEqualTo(1);
	}

	@Test
	public void testOnOrderUpdate() {
		OrderField of = OrderField.newBuilder()
				.setOriginOrderId("adfskal")
				.setTotalVolume(2)
				.setOrderStatus(OrderStatusEnum.OS_Touched)
				.build();
		tda.onOrderUpdate(of);
		assertThat(tda.getTradeDayOrders().size()).isEqualTo(1);
	}

	@Test
	public void testOpenPosition() throws InsufficientException {
		testOnAccountUpdate();
		OrderRequest orderReq = OrderRequest.builder()
				.contractUnifiedSymbol("rb2102@SHFE")
				.price("4000")
				.volume(1)
				.tradeOpr(TradeOperation.BK)
				.gatewayId("testGateway")
				.build();
		tda.openPosition(orderReq);
	}
	
	@Test
	public void testOpenPositionWithException() throws InsufficientException {
		testOnAccountUpdate();
		OrderRequest orderReq = OrderRequest.builder()
				.contractUnifiedSymbol("rb2102@SHFE")
				.price("7000")
				.volume(1)
				.tradeOpr(TradeOperation.BK)
				.gatewayId("testGateway")
				.build();
		assertThrows(InsufficientException.class, ()->{
			tda.openPosition(orderReq);
		});
		
	}

	@Test
	public void testClosePosition() throws InsufficientException {
		testOnPositionUpdate();
		OrderRequest orderReq = OrderRequest.builder()
				.contractUnifiedSymbol("rb2102@SHFE")
				.price("7000")
				.volume(2)
				.tradeOpr(TradeOperation.SP)
				.gatewayId("testGateway")
				.build();
		tda.closePosition(orderReq);
		verify(tda.eventBus, times(2)).post(ArgumentMatchers.any(NorthstarEvent.class));
	}
	
	@Test
	public void testClosePositionWithException() throws InsufficientException {
		testOnPositionUpdate();
		OrderRequest orderReq = OrderRequest.builder()
				.contractUnifiedSymbol("rb2102@SHFE")
				.price("7000")
				.volume(3)
				.tradeOpr(TradeOperation.SP)
				.gatewayId("testGateway")
				.build();
		assertThrows(InsufficientException.class, ()->{			
			tda.closePosition(orderReq);
		});
	}

	@Test
	public void testCancelOrder() throws TradeException {
		testOnOrderUpdate();
		OrderRecall recall = OrderRecall.builder()
				.gatewayId("testGateway")
				.originOrderId("adfskal")
				.build();
		tda.cancelOrder(recall);
		verify(tda.eventBus).post(ArgumentMatchers.any(NorthstarEvent.class));
	}
	
	@Test
	public void testCancelOrderWithException() throws TradeException {
		OrderField of = OrderField.newBuilder()
				.setOrderId("adfskal")
				.setTotalVolume(2)
				.setOrderStatus(OrderStatusEnum.OS_AllTraded)
				.build();
		tda.onOrderUpdate(of);
		
		OrderRecall recall = OrderRecall.builder()
				.gatewayId("testGateway")
				.originOrderId("adfskal")
				.build();
		assertThrows(TradeException.class, ()->{			
			tda.cancelOrder(recall);
		});
	}

}
