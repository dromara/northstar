package tech.xuanwu.northstar.trader.domain.simulated;

import xyz.redtorch.pb.CoreField.AccountField;

/**
 * 模拟盘账户计算
 * @author kevinhuangwl
 *
 */
public class GwAccount {
	
	private volatile AccountField account;
	private GwOrders gwOrders;
	private GwPositions gwPositions;
	
	public GwAccount(AccountField account) {
		this.account = account;
	}
	
	public void setGwPositions(GwPositions gwPositions) {
		this.gwPositions = gwPositions;
	}
	
	public void setGwOrders(GwOrders gwOrders) {
		this.gwOrders = gwOrders;
	}
	
	/**
	 * 入金
	 */
	public synchronized AccountField deposit(double money) throws IllegalArgumentException{
		if(money < 0) {
			throw new IllegalArgumentException("入金金额不能为负数");
		}
		account = account.toBuilder()
				.setDeposit(account.getDeposit() + money)
				.build();
		return getAccount();
	}
	
	/**
	 * 出金
	 */
	public synchronized AccountField withdraw(double money) throws IllegalArgumentException{
		if(money > account.getAvailable() + account.getDeposit()) {
			throw new IllegalArgumentException("出金金额超过可用金额");
		}
		account = account.toBuilder()
				.setWithdraw(account.getWithdraw() + money)
				.build();
		return getAccount();
	}
	
	/**
	 * 获取账户信息
	 * @return
	 */
	public AccountField getAccount() {
		account = refresh();
		return account;
	}
	
	private synchronized AccountField refresh() {
		double useMargin = gwPositions.getTotalUseMargin();
		double frozenAmount = gwOrders.getTotalFrozenAmount();
		
		AccountField.Builder ab = account.toBuilder();
		
		ab.setCloseProfit(gwPositions.getTotalCloseProfit());
		ab.setPositionProfit(gwPositions.getTotalPositionProfit());
		ab.setCommission(gwOrders.getTotalCommission());
		
		//当前权益 = 期初权益 + 当天平仓盈亏  + 持仓盈亏 - 手续费 + 入金金额 - 出金金额 
		ab.setBalance(ab.getPreBalance() + ab.getCloseProfit() + ab.getPositionProfit() - ab.getCommission() + ab.getDeposit() - ab.getWithdraw());
		ab.setMargin(frozenAmount + useMargin);
		
		//可用资金 = 当前权益 - 持仓保证金 - 委托单保证金
		ab.setAvailable(ab.getBalance() - ab.getMargin());
		return ab.build();
	}
	
	/**
	 * 进行日结算
	 */
	public synchronized AccountField proceedDailySettlement() {
		AccountField.Builder ab = account.toBuilder();
		ab.setPreBalance(ab.getPreBalance() + ab.getDeposit() - ab.getWithdraw() + ab.getCloseProfit() - ab.getCommission());
		ab.setCommission(0);
		ab.setCloseProfit(0);
		ab.setDeposit(0);
		ab.setWithdraw(0);
		
		account = refresh();
		return account;
	}
}
