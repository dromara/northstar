package tech.quantit.northstar.gateway.api.domain.contract;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * 组合合约
 * @author KevinHuangwl
 *
 */
@Slf4j
public class GroupedContract implements Contract {

	private List<Contract> memberContracts;
	
	protected GroupedContract(List<Contract> memberContracts) {
		this.memberContracts = memberContracts;
	}

	@Override
	public boolean subscribe() {
		for(Contract c : memberContracts) {
			if(!c.subscribe()) {
				log.warn("[{}] 合约订阅失败", c.contractField().getUnifiedSymbol());
			}
		}
		return true;
	}

	@Override
	public boolean unsubscribe() {
		for(Contract c : memberContracts) {
			if(!c.unsubscribe()) {
				log.warn("[{}] 合约取消订阅失败", c.contractField().getUnifiedSymbol());
			}
		}
		return true;
	}

}
