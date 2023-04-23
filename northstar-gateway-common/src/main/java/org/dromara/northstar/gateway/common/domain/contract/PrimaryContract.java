package org.dromara.northstar.gateway.common.domain.contract;

import java.util.Objects;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.Constants;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.gateway.Contract;
import org.dromara.northstar.gateway.TradeTimeDefinition;

import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
import xyz.redtorch.pb.CoreField.ContractField;

/**
 * 主力合约
 * @author KevinHuangwl
 *
 */
public class PrimaryContract implements Contract {
	
	private IndexContract idxContract;
	
	// 借用指数合约的合约信息
	public PrimaryContract(IndexContract idxContract) {
		this.idxContract = idxContract;
	}

	@Override
	public boolean subscribe() {
		// 主力合约仅作为合约符号用于回测，不能订阅
		return true;
	}

	@Override
	public boolean unsubscribe() {
		// 主力合约仅作为合约符号用于回测，不能退订
		return true;
	}

	@Override
	public String name() {
		return idxContract.name().replace("指数", "主力");
	}

	@Override
	public Identifier identifier() {
		return Identifier.of(idxContract.identifier().value().replace(Constants.INDEX_SUFFIX, Constants.PRIMARY_SUFFIX));
	}

	@Override
	public ProductClassEnum productClass() {
		return idxContract.productClass();
	}

	@Override
	public ExchangeEnum exchange() {
		return idxContract.exchange();
	}

	@Override
	public TradeTimeDefinition tradeTimeDefinition() {
		return idxContract.tradeTimeDefinition();
	}

	@Override
	public ChannelType channelType() {
		return ChannelType.PLAYBACK;
	}

	@Override
	public String gatewayId() {
		return idxContract.gatewayId();
	}

	@Override
	public ContractField contractField() {
		ContractField idxcf = idxContract.contractField();
		return ContractField.newBuilder(idxcf)
				.setName(name())
				.setFullName(idxcf.getFullName().replace("指数", "主力"))
				.setSymbol(idxcf.getSymbol().replace(Constants.INDEX_SUFFIX, Constants.PRIMARY_SUFFIX))
				.setUnifiedSymbol(idxcf.getUnifiedSymbol().replace(Constants.INDEX_SUFFIX, Constants.PRIMARY_SUFFIX))
				.setContractId(idxcf.getContractId().replace(Constants.INDEX_SUFFIX, Constants.PRIMARY_SUFFIX))
				.setThirdPartyId(idxcf.getThirdPartyId().replace(Constants.INDEX_SUFFIX, Constants.PRIMARY_SUFFIX))
				.build();
	}

	@Override
	public int hashCode() {
		return Objects.hash(identifier());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PrimaryContract other = (PrimaryContract) obj;
		return Objects.equals(identifier(), other.identifier());
	}

	
	
}
