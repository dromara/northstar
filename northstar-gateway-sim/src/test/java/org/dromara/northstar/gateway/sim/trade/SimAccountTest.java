package org.dromara.northstar.gateway.sim.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.gateway.api.IContractManager;
import org.dromara.northstar.gateway.api.domain.contract.Contract;
import org.dromara.northstar.gateway.sim.trade.SimAccount;
import org.dromara.northstar.gateway.sim.trade.TradePosition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.ContractField;

class SimAccountTest {
	
	TestFieldFactory factory = new TestFieldFactory("gateway");
	
	SimAccount account;
	
	@SuppressWarnings("unchecked")
	@BeforeEach
	void prepare() {
		Contract contract = mock(Contract.class);
		IContractManager contractMgr = mock(IContractManager.class);
		when(contractMgr.getContract(any(Identifier.class))).thenReturn(contract);
		when(contract.contractField()).thenReturn(ContractField.newBuilder()
				.setCommissionFee(1.5)
				.build());
		account = new SimAccount("testGateway", contractMgr, mock(FastEventEngine.class), mock(Consumer.class));
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
		account.totalDeposit = 1000000;
		account.onSubmitOrder(factory.makeOrderReq("rb2210", DirectionEnum.D_Sell, OffsetFlagEnum.OF_Open, 1, 1000, 0));
		
		assertThat(account.openReqMap).hasSize(1);
	}
	
	@Test
	void shouldMakeOpenOrderWithFailure() {
		account.onSubmitOrder(factory.makeOrderReq("rb2210", DirectionEnum.D_Sell, OffsetFlagEnum.OF_Open, 1, 1000, 0));
		
		assertThat(account.openReqMap).isEmpty();
	}
	
	@Test
	void shouldMakeCloseOrder() {
		TradePosition pos = mock(TradePosition.class);
		when(pos.totalAvailable()).thenReturn(1);
		ConcurrentMap<ContractField, TradePosition> longMap = new ConcurrentHashMap<>();
		longMap.put(factory.makeContract("rb2210"), pos);
		account.longMap = longMap;
		account.onSubmitOrder(factory.makeOrderReq("rb2210", DirectionEnum.D_Sell, OffsetFlagEnum.OF_Close, 1, 1000, 0));
		
		assertThat(account.closeReqMap).hasSize(1);
	}
	
	@Test
	void shouldMakeCloseOrderWithFailure() {
		assertThrows(IllegalStateException.class, ()->{			
			account.onSubmitOrder(factory.makeOrderReq("rb2210", DirectionEnum.D_Sell, OffsetFlagEnum.OF_Close, 1, 1000, 0));
		});
		
		TradePosition pos = mock(TradePosition.class);
		ConcurrentMap<ContractField, TradePosition> longMap = new ConcurrentHashMap<>();
		longMap.put(factory.makeContract("rb2210"), pos);
		account.longMap = longMap;
		account.onSubmitOrder(factory.makeOrderReq("rb2210", DirectionEnum.D_Sell, OffsetFlagEnum.OF_Close, 1, 1000, 0));
		
		assertThat(account.openReqMap).isEmpty();
	}
	
	@Test
	void testDeposit() {
		account.depositMoney(666);
		assertThat(account.totalDeposit).isEqualTo(666);
	}
	
	@Test
	void testDepositFailure() {
		assertThrows(IllegalArgumentException.class, ()->{			
			account.depositMoney(-1);
		});
		
	}
	
	@Test
	void testWithdraw() {
		account.depositMoney(666);
		account.withdrawMoney(333);
		assertThat(account.balance()).isEqualTo(333);
		assertThat(account.totalDeposit).isEqualTo(666);
		assertThat(account.totalWithdraw).isEqualTo(333);
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
