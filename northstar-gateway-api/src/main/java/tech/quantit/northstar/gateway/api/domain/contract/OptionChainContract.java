package tech.quantit.northstar.gateway.api.domain.contract;

import java.util.List;

import org.springframework.util.Assert;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.ChannelType;
import tech.quantit.northstar.common.model.Identifier;
import tech.quantit.northstar.gateway.api.domain.time.TradeTimeDefinition;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;

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
	
	public OptionChainContract(String name, List<Contract> memberContracts) {
		Assert.notEmpty(memberContracts, "集合不能为空");
		this.memberContracts = memberContracts;
		this.identifier = Identifier.of(name);
		this.name = name;
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
}
