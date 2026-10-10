package org.dromara.northstar.app.rl;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentMap;

import org.dromara.northstar.common.model.DealRecord;
import org.dromara.northstar.common.model.core.Account;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.data.IAccountRepository;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

/**
 * 内存态账户仓库：RL 训练沙箱专用。
 *
 * 训练会产生海量成交对账（轮数 × 局数 × 合约），落库既慢又污染业务数据；
 * 训练环境的成交记录只用于当轮评估指标计算，episode 结束即可丢弃。
 */
public class InMemoryAccountRepository implements IAccountRepository {

	private final ConcurrentMap<String, List<DealRecord>> dealRecords = new ConcurrentHashMap<>();

	@Override
	public void saveDealRecord(DealRecord dealRecord) {
		dealRecords.computeIfAbsent(dealRecord.getAccountName(), _ -> new CopyOnWriteArrayList<>()).add(dealRecord);
	}

	@Override
	public List<DealRecord> findDealRecords(String accountName) {
		return List.copyOf(dealRecords.getOrDefault(accountName, List.of()));
	}

	@Override
	public void removeDealRecords(String accountName) {
		dealRecords.remove(accountName);
	}

	@Override
	public void saveAccount(Account account) {
		/* 训练沙箱不需要出入金记录 */
	}

	@Override
	public Account findAccountsAsAggregateAccount(String accountName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeAccounts(String accountName) {
		/* 训练沙箱不需要出入金记录 */
	}

	@Override
	public void saveHoldingTrades(String accountName, Contract contract, PositionDirectionEnum positionDirection,
			List<Trade> holdingTrades) {
		/* 训练沙箱不需要跨进程恢复持仓 */
	}

	@Override
	public Table<Contract, PositionDirectionEnum, List<Trade>> findHoldingTrades(String accountName) {
		return HashBasedTable.create();
	}

	@Override
	public void removeHoldingTrades(String accountName) {
		/* 训练沙箱不需要跨进程恢复持仓 */
	}
}
