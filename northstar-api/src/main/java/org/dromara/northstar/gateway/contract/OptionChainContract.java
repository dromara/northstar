package org.dromara.northstar.gateway.contract;

import java.util.List;
import java.util.Objects;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.Constants;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.gateway.IContract;
import org.springframework.util.Assert;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;

/**
 * 组合合约
 * @author KevinHuangwl
 *
 */
@Slf4j
public class OptionChainContract implements IContract {

	private final String name;
	
	private final List<IContract> memberContracts;
	
	private final Identifier identifier;
	
	public OptionChainContract(IContract underlyingContract, List<IContract> memberContracts) {
		Assert.notEmpty(memberContracts, "集合不能为空");
		this.memberContracts = memberContracts;
		this.identifier = Identifier.of(Constants.OPTION_CHAIN_PREFIX + underlyingContract.identifier().value());
		this.name = underlyingContract.name() + "期权链";
	}

	@Override
	public boolean subscribe() {
		for(IContract c : memberContracts) {
			if(!c.subscribe()) {
				log.warn("[{}] 合约订阅失败", c.contract().unifiedSymbol());
			}
		}
		return true;
	}

	@Override
	public boolean unsubscribe() {
		for(IContract c : memberContracts) {
			if(!c.unsubscribe()) {
				log.warn("[{}] 合约取消订阅失败", c.contract().unifiedSymbol());
			}
		}
		return true;
	}
	
	@Override
	public List<IContract> memberContracts() {
		return memberContracts;
	}

	@Override
	public Contract contract() {
		Contract seed = memberContracts.get(0).contract();
		String unifiedSymbol = String.format("%s@%s@%s", name, seed.exchange(), seed.productClass());
		return seed.toBuilder()
				.name(name)
				.fullName(name)
				.unifiedSymbol(unifiedSymbol)
				.symbol(name)
				.contractId(identifier.value())
				.build();
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public Identifier identifier() {
		return identifier;
	}

	@Override
	public ProductClassEnum productClass() {
		return memberContracts.get(0).productClass();
	}

	@Override
	public ExchangeEnum exchange() {
		return memberContracts.get(0).exchange();
	}

	@Override
	public String gatewayId() {
		return memberContracts.get(0).gatewayId();
	}

	@Override
	public ChannelType channelType() {
		return memberContracts.get(0).channelType();
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OptionChainContract other = (OptionChainContract) obj;
		return Objects.equals(name, other.name);
	}
	
	
}
