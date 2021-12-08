package tech.quantit.northstar.common;

import com.google.common.eventbus.Subscribe;

import xyz.redtorch.pb.CoreField.AccountField;

public interface AccountAware extends Subscribable {

	@Subscribe
	void onAccount(AccountField account);
	
	double accountBalance();
	
	double accountAvailable();
	
}
