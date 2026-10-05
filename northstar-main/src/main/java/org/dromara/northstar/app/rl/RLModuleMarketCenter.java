package org.dromara.northstar.app.rl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.Instrument;
import org.dromara.northstar.gateway.contract.GatewayContract;
import org.dromara.northstar.gateway.mktdata.MarketCenter;

import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;

/**
 * RL 训练沙箱的合约中心：训练合约直接来自数据服务（指数合约），
 * 不经过网关注册与合约定义匹配，仅提供 ModuleContext 所需的合约解析能力。
 */
public class RLModuleMarketCenter extends MarketCenter {

	private final Map<Identifier, IContract> contractMap = new HashMap<>();

	public RLModuleMarketCenter(List<Contract> contracts, IDataSource dataSource) {
		super(List.of(), Map.of());
		for (Contract c : contracts) {
			GatewayContract gc = new GatewayContract(new RLInstrument(c, dataSource));
			contractMap.put(gc.identifier(), gc);
		}
	}

	@Override
	public IContract getContract(Identifier identifier) {
		IContract contract = contractMap.get(identifier);
		if (contract == null) {
			throw new IllegalArgumentException("RL 沙箱未注册合约: " + identifier.value());
		}
		return contract;
	}

	@Override
	public IContract getContract(ChannelType channelType, String code) {
		return contractMap.values().stream()
				.filter(c -> c.channelType() == channelType
						&& (c.contract().symbol().equals(code) || c.contract().unifiedSymbol().equals(code)))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("RL 沙箱未注册合约: " + code));
	}

	/** 最小 Instrument 实现：包装数据服务合约 */
	private record RLInstrument(Contract contract, IDataSource dataSource) implements Instrument {

		@Override
		public String name() {
			return contract.name();
		}

		@Override
		public Identifier identifier() {
			return Identifier.of(contract.contractId());
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
		public ChannelType channelType() {
			return contract.channelType();
		}

		@Override
		public IDataSource dataSource() {
			return dataSource;
		}

		@Override
		public Contract contract() {
			return contract;
		}
	}
}
