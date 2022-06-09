package tech.quantit.northstar.gateway.sim.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.eventbus.EventBus;

import tech.quantit.northstar.common.IContractManager;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.model.ContractDefinition;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.ContractField;

class SimAccountTest {
	
	TestFieldFactory factory = new TestFieldFactory("gateway");
	
	SimAccount account;
	
	@BeforeEach
	void prepare() {
		IContractManager contractMgr = mock(IContractManager.class);
		when(contractMgr.getContractDefinition(anyString())).thenReturn(ContractDefinition.builder()
				.commissionInPrice(1.5)
				.build());
		account = new SimAccount("testGateway", contractMgr);
	}

	@Test
	void testAccountField() {
		
		account.totalCloseProfit = 1000;
		account.totalDeposit = 200;
		account.totalWithdraw = 300;
		account.totalCommission = 60;
		
		AccountField af = account.accountField();
		assertThat(af.getBalance()).isEqualTo(840);
	}

	
	@Test
	void shouldMakeOpenOrder() {
		
		account.setFeEngine(mock(FastEventEngine.class));
		account.totalDeposit = 1000000;
		EventBus eventBus = mock(EventBus.class);
		account.onSubmitOrder(factory.makeOrderReq("rb2210", DirectionEnum.D_Sell, OffsetFlagEnum.OF_Open, 1, 1000, 0));
		
		verify(eventBus).register(any(OpenTradeRequest.class));
		assertThat(account.openReqMap).hasSize(1);
	}
	
	@Test
	void shouldMakeOpenOrderWithFailure() {
		
		account.setFeEngine(mock(FastEventEngine.class));
		EventBus eventBus = mock(EventBus.class);
		account.onSubmitOrder(factory.makeOrderReq("rb2210", DirectionEnum.D_Sell, OffsetFlagEnum.OF_Open, 1, 1000, 0));
		
		verify(eventBus, times(0)).register(any(OpenTradeRequest.class));
		assertThat(account.openReqMap).isEmpty();
	}
	
	@Test
	void shouldMakeCloseOrder() {
		
		account.setFeEngine(mock(FastEventEngine.class));
		EventBus eventBus = mock(EventBus.class);
		
		TradePosition pos = mock(TradePosition.class);
		when(pos.totalAvailable()).thenReturn(1);
		ConcurrentMap<ContractField, TradePosition> longMap = new ConcurrentHashMap<>();
		longMap.put(factory.makeContract("rb2210"), pos);
		account.longMap = longMap;
		account.onSubmitOrder(factory.makeOrderReq("rb2210", DirectionEnum.D_Sell, OffsetFlagEnum.OF_Close, 1, 1000, 0));
		
		verify(eventBus).register(any(CloseTradeRequest.class));
		assertThat(account.closeReqMap).hasSize(1);
	}
	
	@Test
	void shouldMakeCloseOrderWithFailure() {
		
		account.setFeEngine(mock(FastEventEngine.class));
		EventBus eventBus = mock(EventBus.class);
		assertThrows(IllegalStateException.class, ()->{			
			account.onSubmitOrder(factory.makeOrderReq("rb2210", DirectionEnum.D_Sell, OffsetFlagEnum.OF_Close, 1, 1000, 0));
		});
		
		TradePosition pos = mock(TradePosition.class);
		ConcurrentMap<ContractField, TradePosition> longMap = new ConcurrentHashMap<>();
		longMap.put(factory.makeContract("rb2210"), pos);
		account.longMap = longMap;
		account.onSubmitOrder(factory.makeOrderReq("rb2210", DirectionEnum.D_Sell, OffsetFlagEnum.OF_Close, 1, 1000, 0));
		
		verify(eventBus, times(0)).register(any(OpenTradeRequest.class));
		assertThat(account.openReqMap).isEmpty();
	}
	
	@Test
	void testDeposit() {
		
		Runnable savingCallback = mock(Runnable.class);
		account.setSavingCallback(savingCallback);
		account.setFeEngine(mock(FastEventEngine.class));
		account.depositMoney(666);
		assertThat(account.totalDeposit).isEqualTo(666);
		verify(savingCallback).run();
	}
	
	@Test
	void testDepositFailure() {
		
		assertThrows(IllegalArgumentException.class, ()->{			
			account.depositMoney(-1);
		});
		
	}
	
	@Test
	void testWithdraw() {
		
		Runnable savingCallback = mock(Runnable.class);
		account.setFeEngine(mock(FastEventEngine.class));
		account.setSavingCallback(savingCallback);
		account.depositMoney(666);
		account.withdrawMoney(333);
		assertThat(account.balance()).isEqualTo(333);
		assertThat(account.totalDeposit).isEqualTo(666);
		assertThat(account.totalWithdraw).isEqualTo(333);
		verify(savingCallback, times(2)).run();
	}
	
	@Test
	void testWithdrawFailure() {
		
		assertThrows(IllegalArgumentException.class, ()->{			
			account.withdrawMoney(-1);
		});
		assertThrows(IllegalStateException.class, ()->{			
			account.withdrawMoney(10);
		});
	}
	
	@Test
	void testOnCancel() {
		account.onCancelOrder(factory.makeCancelReq(factory.makeOrderReq("rb2201", DirectionEnum.D_Buy, OffsetFlagEnum.OF_Close, 0, 0, 0)));
	}
	
}
