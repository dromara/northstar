package tech.quantit.northstar.gateway.sim.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.jupiter.api.Test;

import com.google.common.eventbus.EventBus;

import tech.quantit.northstar.common.event.FastEventEngine;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.AccountField;

class SimAccountTest {
	
	TestFieldFactory factory = new TestFieldFactory("gateway");

	@Test
	void testAccountField() {
		SimAccount account = new SimAccount("test", 3);
		account.totalCloseProfit = 1000;
		account.totalDeposit = 200;
		account.totalWithdraw = 300;
		account.totalCommission = 60;
		
		AccountField af = account.accountField();
		assertThat(af.getBalance()).isEqualTo(840);
	}

	
	@Test
	void shouldMakeOpenOrder() {
		SimAccount account = new SimAccount("test", 3);
		account.setFeEngine(mock(FastEventEngine.class));
		account.totalDeposit = 1000000;
		EventBus eventBus = mock(EventBus.class);
		account.setEventBus(eventBus);
		account.onSubmitOrder(factory.makeOrderReq("rb2210", DirectionEnum.D_Sell, OffsetFlagEnum.OF_Open, 1, 1000, 0));
		
		verify(eventBus).register(any(OpenTradeRequest.class));
		assertThat(account.openReqSet).hasSize(1);
	}
	
	@Test
	void shouldMakeOpenOrderWithFailure() {
		SimAccount account = new SimAccount("test", 3);
		account.setFeEngine(mock(FastEventEngine.class));
		EventBus eventBus = mock(EventBus.class);
		account.setEventBus(eventBus);
		account.onSubmitOrder(factory.makeOrderReq("rb2210", DirectionEnum.D_Sell, OffsetFlagEnum.OF_Open, 1, 1000, 0));
		
		verify(eventBus, times(0)).register(any(OpenTradeRequest.class));
		assertThat(account.openReqSet).isEmpty();
	}
	
	@Test
	void shouldMakeCloseOrder() {
		SimAccount account = new SimAccount("test", 3);
		account.setFeEngine(mock(FastEventEngine.class));
		EventBus eventBus = mock(EventBus.class);
		account.setEventBus(eventBus);
		
		SimPosition pos = mock(SimPosition.class);
		when(pos.availableVol()).thenReturn(1);
		ConcurrentMap<String, SimPosition> longMap = new ConcurrentHashMap<>();
		longMap.put("rb2210@SHFE@FUTURES", pos);
		account.longMap = longMap;
		account.onSubmitOrder(factory.makeOrderReq("rb2210", DirectionEnum.D_Sell, OffsetFlagEnum.OF_Close, 1, 1000, 0));
		
		verify(eventBus).register(any(CloseTradeRequest.class));
		assertThat(account.closeReqSet).hasSize(1);
	}
	
	@Test
	void shouldMakeCloseOrderWithFailure() {
		SimAccount account = new SimAccount("test", 3);
		account.setFeEngine(mock(FastEventEngine.class));
		EventBus eventBus = mock(EventBus.class);
		account.setEventBus(eventBus);
		assertThrows(IllegalStateException.class, ()->{			
			account.onSubmitOrder(factory.makeOrderReq("rb2210", DirectionEnum.D_Sell, OffsetFlagEnum.OF_Close, 1, 1000, 0));
		});
		
		SimPosition pos = mock(SimPosition.class);
		ConcurrentMap<String, SimPosition> longMap = new ConcurrentHashMap<>();
		longMap.put("rb2210@SHFE@FUTURES", pos);
		account.longMap = longMap;
		account.onSubmitOrder(factory.makeOrderReq("rb2210", DirectionEnum.D_Sell, OffsetFlagEnum.OF_Close, 1, 1000, 0));
		
		verify(eventBus, times(0)).register(any(OpenTradeRequest.class));
		assertThat(account.openReqSet).isEmpty();
	}
	
	@Test
	void testDeposit() {
		SimAccount account = new SimAccount("test", 3);
		Runnable savingCallback = mock(Runnable.class);
		account.setSavingCallback(savingCallback);
		account.depositMoney(666);
		assertThat(account.totalDeposit).isEqualTo(666);
		verify(savingCallback).run();
	}
	
	@Test
	void testDepositFailure() {
		SimAccount account = new SimAccount("test", 3);
		assertThrows(IllegalArgumentException.class, ()->{			
			account.depositMoney(-1);
		});
		
	}
	
	@Test
	void testWithdraw() {
		SimAccount account = new SimAccount("test", 3);
		Runnable savingCallback = mock(Runnable.class);
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
		SimAccount account = new SimAccount("test", 3);
		assertThrows(IllegalArgumentException.class, ()->{			
			account.withdrawMoney(-1);
		});
		assertThrows(IllegalStateException.class, ()->{			
			account.withdrawMoney(10);
		});
	}
	
	@Test
	void testOnCancel() {
		SimAccount account = new SimAccount("test", 3);
		account.setEventBus(mock(EventBus.class));
		account.onCancelOrder(factory.makeCancelReq(factory.makeOrderReq("rb2201", DirectionEnum.D_Buy, OffsetFlagEnum.OF_Close, 0, 0, 0)));
		verify(account.getEventBus()).post(any());
	}
	
	@Test
	void testSetEventBus() {
		SimPosition pos = mock(SimPosition.class);
		ConcurrentMap<String, SimPosition> longMap = new ConcurrentHashMap<>();
		longMap.put("any", pos);
		SimAccount account = new SimAccount("test", 3);
		account.longMap = longMap;
		account.setSavingCallback(mock(Runnable.class));
		verify(pos).setSavingCallback(any(Runnable.class));
	}
}
