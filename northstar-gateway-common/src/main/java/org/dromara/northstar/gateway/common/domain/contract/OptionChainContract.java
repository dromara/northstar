package org.dromara.northstar.gateway.common.domain.contract;

import java.util.List;
import java.util.Objects;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.Constants;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.gateway.Contract;
import org.dromara.northstar.gateway.TradeTimeDefinition;
import org.springframework.util.Assert;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
import xyz.redtorch.pb.CoreField.ContractField;

/**
 * 组合合约
 * @author KevinHuangwl
 *
 */
@Slf4j
public class OptionChainContract implements Contract {

	private final String name;
	
	private final List<Contract> memberContracts;
	
	private final Identifier identifier;
	
	public OptionChainContract(Contract underlyingContract, List<Contract> memberContracts) {
		Assert.notEmpty(memberContracts, "集合不能为空");
		this.memberContracts = memberContracts;
		this.identifier = Identifier.of(Constants.OPTION_CHAIN_PREFIX + underlyingContract.identifier().value());
		this.name = underlyingContract.name() + "期权链";
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
	
	@Override
	public List<Contract> memberContracts() {
		return memberContracts;
	}

	@Override
	public ContractField contractField() {
		ContractField seed = memberContracts.get(0).contractField();
		String unifiedSymbol = String.format("%s@%s@%s", name, seed.getExchange(), seed.getProductClass());
		return ContractField.newBuilder(seed)
				.setName(name)
				.setFullName(name)
				.setUnifiedSymbol(unifiedSymbol)
				.setSymbol(name)
				.setContractId(identifier.value())
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
	public TradeTimeDefinition tradeTimeDefinition() {
		return memberContracts.get(0).tradeTimeDefinition();
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
