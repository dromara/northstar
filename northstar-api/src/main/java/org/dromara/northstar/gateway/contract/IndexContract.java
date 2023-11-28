package org.dromara.northstar.gateway.contract;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.Constants;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.TradeTimeDefinition;
import org.dromara.northstar.gateway.mktdata.IndexTicker;
import org.dromara.northstar.gateway.mktdata.MinuteBarGenerator;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 指数合约
 * @author KevinHuangwl
 *
 */
@Slf4j
public class IndexContract implements IContract, TickDataAware{

	private final List<IContract> monthContracts;
	
	private final MinuteBarGenerator barGen;
	
	private final IndexTicker ticker;
	
	private final ContractField contract;
	
	private final Identifier identifier;
	
	private final IDataSource dataSrc;
	
	public IndexContract(FastEventEngine feEngine, List<IContract> monthContracts) {
		this.monthContracts = monthContracts;
		this.contract = makeIndexContractField(monthContracts.get(0).contractField());
		this.dataSrc = monthContracts.get(0).dataSource();
		this.identifier = Identifier.of(contract.getContractId());
		this.barGen = new MinuteBarGenerator(contract, monthContracts.get(0).tradeTimeDefinition(), bar -> feEngine.emitEvent(NorthstarEventType.BAR, bar));
		this.ticker = new IndexTicker(this, t -> {
			feEngine.emitEvent(NorthstarEventType.TICK, t);
			barGen.update(t);
		});
	}
	
	private ContractField makeIndexContractField(ContractField proto) {
		String name = proto.getName().replaceAll("\\d+$", "指数");
		String fullName = proto.getFullName().replaceAll("\\d+$", "指数");
		String originSymbol = proto.getSymbol();
		String symbol = originSymbol.replaceAll("\\d+$", Constants.INDEX_SUFFIX);
		String contractId = proto.getContractId().replace(originSymbol, symbol);
		String thirdPartyId = proto.getThirdPartyId().replace(originSymbol, symbol);
		String unifiedSymbol = proto.getUnifiedSymbol().replace(originSymbol, symbol);
		return ContractField.newBuilder(proto)
				.setSymbol(symbol)
				.setThirdPartyId(thirdPartyId)
				.setContractId(contractId)
				.setLastTradeDateOrContractMonth("")
				.setUnifiedSymbol(unifiedSymbol)
				.setFullName(fullName)
				.setLongMarginRatio(0.1)
				.setShortMarginRatio(0.1)
				.setName(name)
				.build();
	}
	

	@Override
	public IDataSource dataSource() {
		return dataSrc;
	}

	@Override
	public boolean subscribe() {
		log.debug("订阅：{}", identifier.value());
		for(IContract c : monthContracts) {
			if(!c.subscribe()) {
				log.warn("[{}] 合约订阅失败", c.contractField().getUnifiedSymbol());
			}
		}
		return true;
	}

	@Override
	public boolean unsubscribe() {
		log.debug("退订：{}", identifier.value());
		for(IContract c : monthContracts) {
			if(!c.unsubscribe()) {
				log.warn("[{}] 合约取消订阅失败", c.contractField().getUnifiedSymbol());
			}
		}
		return true;
	}
	
	@Override
	public void onTick(TickField tick) {
		ticker.update(tick);
	}

	@Override
	public List<IContract> memberContracts() {
		return monthContracts;
	}

	@Override
	public ContractField contractField() {
		return contract;
	}

	@Override
	public String name() {
		return contract.getName();
	}

	@Override
	public Identifier identifier() {
		return identifier;
	}

	@Override
	public ProductClassEnum productClass() {
		return contract.getProductClass();
	}

	@Override
	public ExchangeEnum exchange() {
		return contract.getExchange();
	}

	@Override
	public String gatewayId() {
		return contract.getGatewayId();
	}

	@Override
	public TradeTimeDefinition tradeTimeDefinition() {
		return monthContracts.get(0).tradeTimeDefinition();
	}

	@Override
	public ChannelType channelType() {
		return monthContracts.get(0).channelType();
	}

	@Override
	public void endOfMarket() {
		barGen.forceEndOfBar();
	}

	@Override
	public int hashCode() {
		return Objects.hash(contract.getContractId());
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
		return StringUtils.equals(contract.getContractId(), other.contract.getContractId());
	}
	
}
