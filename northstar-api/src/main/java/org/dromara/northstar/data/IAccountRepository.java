package org.dromara.northstar.data;

import java.util.List;

import org.dromara.northstar.common.model.DealRecord;
import org.dromara.northstar.common.model.core.Account;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Trade;

import com.google.common.collect.Table;

import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

public interface IAccountRepository {

	/**
	 * 保存一笔开平仓记录
	 * @param dealRecord
	 */
	void saveDealRecord(DealRecord dealRecord);
	
	/**
	 * 获取某个账户的所有开平仓记录
	 * @param hostName
	 * @return
	 */
	List<DealRecord> findDealRecords(String accountName);
	
	/**
	 * 移除某个账户的所有开平仓记录
	 * @param accountName
	 */
	void removeDealRecords(String accountName);
	
	/**
	 * 保存一笔出入金记录
	 */
	void saveAccount(Account account);
	
	/**
	 * 获取某个账户的所有出入金记录并聚会返回
	 * @return
	 */
	Account findAccountsAsAggregateAccount(String accountName);
	
	/**
	 * 移除某个账户的所有出入金记录
	 */
	void removeAccounts(String accountName);
	
	/**
	 * 保存某个账户某个合约的多空未平仓交易记录
	 * @param accountName
	 * @param trades
	 */
	void saveHoldingTrades(String accountName, Contract contract, PositionDirectionEnum positionDirection, List<Trade> holdingTrades);
	
	/**
	 * 获取某个账户的未平仓交易记录
	 * @param accountName
	 * @return
	 */
	Table<Contract, PositionDirectionEnum, List<Trade>> findHoldingTrades(String accountName);
	
	/**
	 * 移除某个账户的未平仓交易记录
	 * @param accountName
	 */
	void removeHoldingTrades(String accountName);
	
}
