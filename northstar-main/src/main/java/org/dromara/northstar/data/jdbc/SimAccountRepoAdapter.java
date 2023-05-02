package org.dromara.northstar.data.jdbc;

import org.dromara.northstar.common.model.SimAccountDescription;
import org.dromara.northstar.data.ISimAccountRepository;
import org.dromara.northstar.data.jdbc.model.SimAccountDescriptionDO;

public class SimAccountRepoAdapter implements ISimAccountRepository {
	
	private SimAccountRepository delegate;
	
	public SimAccountRepoAdapter(SimAccountRepository delegate) {
		this.delegate = delegate;
	}

	@Override
	public void save(SimAccountDescription simAccountDescription) {
		delegate.save(SimAccountDescriptionDO.convertFrom(simAccountDescription));
	}

	@Override
	public SimAccountDescription findById(String accountId) {
		return delegate.findById(accountId).orElseThrow().convertTo();
	}

	@Override
	public void deleteById(String accountId) {
		delegate.deleteById(accountId);
	}

}
