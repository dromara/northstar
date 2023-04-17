package org.dromara.northstar.common;

import xyz.redtorch.pb.CoreField.AccountField;

public interface AccountAware {

	
	void onAccount(AccountField account);
	
	double accountBalance();
	
	double accountAvailable();
	
}
