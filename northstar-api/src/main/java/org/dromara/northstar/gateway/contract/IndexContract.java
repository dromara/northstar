package org.dromara.northstar.gateway.contract;

import java.time.LocalDate;
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
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.mktdata.IndexTicker;
import org.dromara.northstar.gateway.mktdata.MinuteBarGenerator;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;

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
	
	private final Contract contract;
	
	private final Identifier identifier;
	
	private final IDataSource dataSrc;
	
	public IndexContract(FastEventEngine feEngine, List<IContract> monthContracts) {
		this.monthContracts = monthContracts;
		this.contract = makeIndexContractField(monthContracts.get(0).contract());
		this.dataSrc = monthContracts.get(0).dataSource();
		this.identifier = Identifier.of(contract.contractId());
		this.barGen = new MinuteBarGenerator(contract, bar -> feEngine.emitEvent(NorthstarEventType.BAR, bar));
		this.ticker = new IndexTicker(this, t -> {
			feEngine.emitEvent(NorthstarEventType.TICK, t);
			barGen.update(t);
		});
	}
	
	private Contract makeIndexContractField(Contract proto) {
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
				.longMarginRatio(proto.longMarginRatio())
				.shortMarginRatio(proto.shortMarginRatio())
				.tradable(false)
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
				log.warn("[{}] 合约订阅失败", c.contract().unifiedSymbol());
			}
		}
		return true;
	}

	@Override
	public boolean unsubscribe() {
		log.debug("退订：{}", identifier.value());
		for(IContract c : monthContracts) {
			if(!c.unsubscribe()) {
				log.warn("[{}] 合约取消订阅失败", c.contract().unifiedSymbol());
			}
		}
		return true;
	}
	
	@Override
	public void onTick(Tick tick) {
		ticker.update(tick);
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
