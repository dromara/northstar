package org.dromara.northstar.data.jdbc;

import java.util.Collections;
import java.util.Objects;

import org.dromara.northstar.common.model.SimAccountDescription;
import org.dromara.northstar.data.ISimAccountRepository;
import org.dromara.northstar.data.jdbc.entity.SimAccountDescriptionDO;

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
		SimAccountDescriptionDO obj = delegate.findById(accountId).orElse(null);
		if(Objects.isNull(obj)) {
			SimAccountDescription simAccount = new SimAccountDescription();
			simAccount.setGatewayId(accountId);
			simAccount.setOpenTrades(Collections.emptyList());
			return simAccount;
		}
		return obj.convertTo();
	}

	@Override
	public void deleteById(String accountId) {
		if(delegate.existsById(accountId)) {
			delegate.deleteById(accountId);
		}
	}

}
