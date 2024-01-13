package org.dromara.northstar.gateway.contract;

import java.util.Objects;

import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.Constants;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.gateway.IContract;

import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;

/**
 * 主连合约
 * @author KevinHuangwl
 *
 */
public class PrimaryContract implements IContract {
	
	private IndexContract idxContract;
	
	// 借用指数合约的合约信息
	public PrimaryContract(IndexContract idxContract) {
		this.idxContract = idxContract;
	}
	
	@Override
	public IDataSource dataSource() {
		return idxContract.dataSource();
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
		return idxContract.name().replace("指数", "主连");
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
	public ChannelType channelType() {
		return ChannelType.PLAYBACK;
	}

	@Override
	public String gatewayId() {
		return idxContract.gatewayId();
	}

	@Override
	public Contract contract() {
		Contract idxcf = idxContract.contract();
		return idxcf.toBuilder()
				.name(name())
				.fullName(idxcf.fullName().replace("指数", "主力"))
				.symbol(idxcf.symbol().replace(Constants.INDEX_SUFFIX, Constants.PRIMARY_SUFFIX))
				.unifiedSymbol(idxcf.unifiedSymbol().replace(Constants.INDEX_SUFFIX, Constants.PRIMARY_SUFFIX))
				.contractId(idxcf.contractId().replace(Constants.INDEX_SUFFIX, Constants.PRIMARY_SUFFIX))
				.thirdPartyId(idxcf.thirdPartyId().replace(Constants.INDEX_SUFFIX, Constants.PRIMARY_SUFFIX))
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
