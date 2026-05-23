package org.dromara.northstar.gateway.contract;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.Constants;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.gateway.IContract;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;

/**
 * 指数合约
 * @author KevinHuangwl
 *
 */
@Slf4j
public class IndexContract implements IContract {

	private final List<IContract> monthContracts;
	
	private final Contract contract;
	
	private final Identifier identifier;
	
	private final IDataSource dataSrc;
	
	public IndexContract(List<IContract> monthContracts) {
		this.monthContracts = monthContracts;
		this.contract = makeIndexContract(monthContracts.get(0).contract());
		this.dataSrc = monthContracts.get(0).dataSource();
		this.identifier = Identifier.of(contract.contractId());
	}
	
	private Contract makeIndexContract(Contract proto) {
		String name = proto.name().replaceAll("\\d+$", "指数");
		String fullName = proto.fullName().replaceAll("\\d+$", "指数");
		String originSymbol = proto.symbol();
		String symbol = originSymbol.replaceAll("\\d+$", Constants.INDEX_SUFFIX);
		String contractId = proto.contractId().replace(originSymbol, symbol);
		String thirdPartyId = proto.thirdPartyId().replace(originSymbol, symbol);
		String unifiedSymbol = proto.unifiedSymbol().replace(originSymbol, symbol);
		return proto.toBuilder()
				.symbol(symbol)
				.thirdPartyId(thirdPartyId)
				.contractId(contractId)
				.lastTradeDate(LocalDate.MAX)
				.unifiedSymbol(unifiedSymbol)
				.name(name)
				.fullName(fullName)
				.onboardingDate(LocalDate.of(2000, 1, 1))
				.tradable(false)
				.build();
	}
	

	@Override
	public IDataSource dataSource() {
		return dataSrc;
	}

	@Override
	public List<IContract> memberContracts() {
		return monthContracts;
	}

	@Override
	public Contract contract() {
		return contract;
	}

	@Override
	public String name() {
		return contract.name();
	}

	@Override
	public Identifier identifier() {
		return identifier;
	}

	@Override
	public ProductClassEnum productClass() {
		return contract.productClass();
	}

	@Override
	public ExchangeEnum exchange() {
		return contract.exchange();
	}

	@Override
	public String gatewayId() {
		return contract.gatewayId();
	}

	@Override
	public ChannelType channelType() {
		return monthContracts.get(0).channelType();
	}

	@Override
	public int hashCode() {
		return Objects.hash(contract.contractId());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IndexContract other = (IndexContract) obj;
		return StringUtils.equals(contract.contractId(), other.contract.contractId());
	}
	
}
