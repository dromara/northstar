package org.dromara.northstar.data.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.dromara.northstar.common.model.DealRecord;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.core.Account;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.data.IAccountRepository;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.alibaba.fastjson2.JSON;

import xyz.redtorch.pb.CoreEnum.CurrencyEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

@DataJpaTest
class AccountRepositoryAdapterTest {

	@Autowired
	private DealRecordRepository dealRecordRepo;
	
	@Autowired
	private HoldingsRepository holdingsRepo;
	
	@Autowired
	private AccountRepository accountRepo;
	
	private IAccountRepository repo;
	
	String accountName = "testAccount";
	
	DealRecord dealRecord = DealRecord.builder()
			.accountName(accountName)
			.dealProfit(1000)
			.transactionFee(10)
			.build();
	
	Account account = Account.builder()
			.currency(CurrencyEnum.CNY)
			.gatewayId(accountName)
			.preBalance(0)
			.balance(100000)
			.available(80000)
			.commission(0)
			.margin(0)
			.realizedProfit(0)
			.unrealizedProfit(0)
			.deposit(50000)
			.withdraw(0)
			.build();
	
	Contract contract = Contract.builder().contractId("rb2210").unifiedSymbol("rb2210").build();
	
	Trade trade = Trade.builder()
			.gatewayId(accountName)
			.contract(contract)
			.volume(1)
			.price(3500)
			.direction(DirectionEnum.D_Buy)
			.offsetFlag(OffsetFlagEnum.OF_Open)
			.build();
	
	@BeforeEach
	void prepare() {
		MarketCenter mktCenter = mock(MarketCenter.class);
		IContract iContract = mock(IContract.class);
		when(iContract.contract()).thenReturn(contract);
		when(mktCenter.getContract(Identifier.of("rb2210"))).thenReturn(iContract);
		repo = new AccountRepositoryAdapter(dealRecordRepo, holdingsRepo, accountRepo, mktCenter);
		dealRecordRepo.deleteAll();
		holdingsRepo.deleteAll();
		accountRepo.deleteAll();
	}
	
	@Test
	void testSaveDealRecord() {
		assertDoesNotThrow(() -> repo.saveDealRecord(dealRecord));
	}
	
	@Test
	void testFindDealRecord() {
		repo.saveDealRecord(dealRecord);
		List<DealRecord> results = repo.findDealRecords(accountName);
		assertThat(results).hasSize(1);
		assertThat(results.get(0).getDealProfit()).isEqualTo(1000);
	}
	
	@Test
	void testRemoveDealRecords() {
		repo.saveDealRecord(dealRecord);
		repo.removeDealRecords(accountName);
		assertThat(repo.findDealRecords(accountName)).isEmpty();
	}
	
	@Test
	void testSaveAccount() {
		assertDoesNotThrow(() -> repo.saveAccount(account));
	}
	
	@Test
	void testFindAccountsAsAggregateAccount() {
		repo.saveAccount(account);
		Account result = repo.findAccountsAsAggregateAccount(accountName);
		assertThat(result).isNotNull();
		assertThat(result.balance()).isEqualTo(100000);
		assertThat(result.deposit()).isEqualTo(50000);
	}
	
	@Test
	void testFindAccountsAsAggregateAccountWithMultipleRecords() throws InterruptedException {
		String accountId = UUID.randomUUID().toString();
		Account account1 = Account.builder()
				.currency(CurrencyEnum.CNY)
				.gatewayId(accountId)
				.preBalance(0)
				.balance(100022)
				.available(80000)
				.commission(0)
				.margin(2000)
				.realizedProfit(0)
				.unrealizedProfit(0)
				.deposit(50000)
				.withdraw(0)
				.build();
		Account account2 = Account.builder()
				.currency(CurrencyEnum.CNY)
				.gatewayId(accountId)
				.preBalance(0)
				.balance(51100)
				.available(30000)
				.commission(0)
				.margin(5000)
				.realizedProfit(0)
				.unrealizedProfit(0)
				.deposit(20000)
				.withdraw(10000)
				.build();
		repo.saveAccount(account1);
		repo.saveAccount(account2);
		
		Account result = repo.findAccountsAsAggregateAccount(accountId);
		System.out.println(JSON.toJSONString(result));
		assertThat(result).isNotNull();
		assertThat(result.balance()).isEqualTo(151122);
		assertThat(result.available()).isEqualTo(110000);
		assertThat(result.margin()).isEqualTo(7000);
		assertThat(result.deposit()).isEqualTo(70000);
		assertThat(result.withdraw()).isEqualTo(10000);
		
		repo.removeAccounts(accountId);
	}
	
	@Test
	void testRemoveAccounts() {
		repo.saveAccount(account);
		repo.removeAccounts(accountName);
		assertThat(repo.findAccountsAsAggregateAccount(accountName)).isNull();
	}
	
	@Test
	void testSaveHoldingTrades() {
		assertDoesNotThrow(() -> repo.saveHoldingTrades(accountName, contract, PositionDirectionEnum.PD_Long, List.of(trade)));
	}
	
	@Test
	void testFindHoldingTrades() {
		repo.saveHoldingTrades(accountName, contract, PositionDirectionEnum.PD_Long, List.of(trade));
		var results = repo.findHoldingTrades(accountName);
		assertThat(results.get(contract, PositionDirectionEnum.PD_Long)).hasSize(1);
		assertThat(results.get(contract, PositionDirectionEnum.PD_Long).get(0).gatewayId()).isEqualTo(accountName);
	}
	
	@Test
	void testRemoveHoldingTrades() {
		repo.saveHoldingTrades(accountName, contract, PositionDirectionEnum.PD_Long, List.of(trade));
		repo.removeHoldingTrades(accountName);
		assertThat(repo.findHoldingTrades(accountName).isEmpty()).isTrue();
	}
}
