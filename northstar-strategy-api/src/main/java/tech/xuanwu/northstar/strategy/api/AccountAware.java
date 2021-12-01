package tech.xuanwu.northstar.strategy.api;

import com.google.common.eventbus.Subscribe;

import xyz.redtorch.pb.CoreField.AccountField;

public interface AccountAware {

	@Subscribe
	void onAccount(AccountField account);
	
	double accountBalance();
	
	double accountAvailable();
	
}
