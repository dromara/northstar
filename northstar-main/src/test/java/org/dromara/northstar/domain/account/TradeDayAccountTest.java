package org.dromara.northstar.domain.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.dromara.northstar.domain.account.TradeDayAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.quantit.northstar.common.exception.InsufficientException;
import tech.quantit.northstar.common.exception.TradeException;
import tech.quantit.northstar.common.model.Identifier;
import tech.quantit.northstar.common.model.OrderRecall;
import tech.quantit.northstar.common.model.OrderRequest;
import tech.quantit.northstar.common.model.OrderRequest.TradeOperation;
import tech.quantit.northstar.gateway.api.IContractManager;
import tech.quantit.northstar.gateway.api.TradeGateway;
import tech.quantit.northstar.gateway.api.domain.contract.Contract;
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
		TradeGateway gateway = mock(TradeGateway.class);
		IContractManager contractMgr = mock(IContractManager.class);
		Contract c = mock(Contract.class);
		when(c.contractField()).thenReturn(contract);
		when(contractMgr.getContract(Identifier.of("rb2102@SHFE"))).thenReturn(c);
		tda = new TradeDayAccount("testGateway", gateway, contractMgr);
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
				.contractId("rb2102@SHFE")
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
				.contractId("rb2102@SHFE")
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
				.contractId("rb2102@SHFE")
				.price("7000")
				.volume(2)
				.tradeOpr(TradeOperation.SP)
				.gatewayId("testGateway")
				.build();
		tda.closePosition(orderReq);
		verify(tda.gateway, times(2)).submitOrder(any());
	}
	
	@Test
	public void testClosePositionWithException() throws InsufficientException {
		testOnPositionUpdate();
		OrderRequest orderReq = OrderRequest.builder()
				.contractId("rb2102@SHFE")
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
		verify(tda.gateway).cancelOrder(any());
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
