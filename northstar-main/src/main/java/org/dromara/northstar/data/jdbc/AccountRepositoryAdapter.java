package org.dromara.northstar.data.jdbc;

import java.util.List;

import org.dromara.northstar.common.model.DealRecord;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.core.Account;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.data.IAccountRepository;
import org.dromara.northstar.data.jdbc.entity.AccountDO;
import org.dromara.northstar.data.jdbc.entity.DealRecordDO;
import org.dromara.northstar.data.jdbc.entity.HoldingTradesDO;
import org.dromara.northstar.gateway.mktdata.MarketCenter;

import com.alibaba.fastjson2.JSON;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

public class AccountRepositoryAdapter implements IAccountRepository{
	
	private DealRecordRepository dealRepo;
	
	private HoldingsRepository holdingsRepo;
	
	private AccountRepository accountRepo;
	
	private MarketCenter mktCenter;
	
	public AccountRepositoryAdapter(DealRecordRepository dealRepo, HoldingsRepository holdingsRepo, AccountRepository accountRepo,
			MarketCenter mktCenter) {
		this.dealRepo = dealRepo;
		this.holdingsRepo = holdingsRepo;
		this.accountRepo = accountRepo;
		this.mktCenter = mktCenter;
	}
	
	@Override
	public void saveDealRecord(DealRecord dealRecord) {
		dealRepo.save(DealRecordDO.convertFrom(dealRecord));
	}

	@Override
	public List<DealRecord> findDealRecords(String accountName) {
		return dealRepo.findByAccountName(accountName).stream()
				.map(DealRecordDO::convertTo)
				.toList();
	}

	@Override
	public void removeDealRecords(String accountName) {
		dealRepo.deleteByAccountName(accountName);
	}

	@Override
	public void saveAccount(Account account) {
		accountRepo.save(AccountDO.convertFrom(account.gatewayId(), account));
	}

	@Override
	public Account findAccountsAsAggregateAccount(String accountName) {
		List<AccountDO> accountDOList = accountRepo.findByAccountName(accountName);
		if (accountDOList.isEmpty()) {
			return null;
		}
		List<Account> accounts = accountDOList.stream()
				.map(AccountDO::convertTo)
				.toList();
		
		double preBalance = 0;
		double balance = 0;
		double available = 0;
		double commission = 0;
		double margin = 0;
		double realizedProfit = 0;
		double unrealizedProfit = 0;
		double deposit = 0;
		double withdraw = 0;
		
		for (Account acc : accounts) {
			preBalance += acc.preBalance();
			balance += acc.balance();
			available += acc.available();
			commission += acc.commission();
			margin += acc.margin();
			realizedProfit += acc.realizedProfit();
			unrealizedProfit += acc.unrealizedProfit();
			deposit += acc.deposit();
			withdraw += acc.withdraw();
		}
		
		return Account.builder()
				.currency(accounts.get(0).currency())
				.preBalance(preBalance)
				.balance(balance)
				.available(available)
				.commission(commission)
				.margin(margin)
				.realizedProfit(realizedProfit)
				.unrealizedProfit(unrealizedProfit)
				.deposit(deposit)
				.withdraw(withdraw)
				.gatewayId(accountName)
				.build();
	}

	@Override
	public void removeAccounts(String accountName) {
		accountRepo.deleteByAccountName(accountName);
	}

	@Override
	public void saveHoldingTrades(String accountName, Contract contract, PositionDirectionEnum dir,  List<Trade> trades) {
		holdingsRepo.save(HoldingTradesDO.builder()
				.id(accountName + "_" + contract.contractId())
				.accountName(accountName)
				.contractId(contract.contractId())
				.positionDir(dir)
				.dataStr(JSON.toJSONString(trades))
				.build());
	}

	@Override
	public Table<Contract, PositionDirectionEnum, List<Trade>> findHoldingTrades(String accountName) {
		Table<Contract, PositionDirectionEnum, List<Trade>> resultTbl = HashBasedTable.create();
		List<HoldingTradesDO> resultList = holdingsRepo.findByAccountName(accountName);
		List<Contract> contracts = resultList.stream().map(tdo -> mktCenter.getContract(Identifier.of(tdo.getContractId())).contract()).toList();
		for(Contract contract : contracts) {
			resultTbl.put(contract, PositionDirectionEnum.PD_Long, resultList.stream()
					.filter(ht -> ht.getContractId().equals(contract.contractId()))
					.filter(ht -> ht.getPositionDir() == PositionDirectionEnum.PD_Long)
					.flatMap(ht -> ht.convertTo(mktCenter).stream())
					.toList());
			resultTbl.put(contract, PositionDirectionEnum.PD_Short, resultList.stream()
					.filter(ht -> ht.getContractId().equals(contract.contractId()))
					.filter(ht -> ht.getPositionDir() == PositionDirectionEnum.PD_Short)
					.flatMap(ht -> ht.convertTo(mktCenter).stream())
					.toList());
		}
		return resultTbl;
	}

	@Override
	public void removeHoldingTrades(String accountName) {
		holdingsRepo.deleteByAccountName(accountName);
	}

}
