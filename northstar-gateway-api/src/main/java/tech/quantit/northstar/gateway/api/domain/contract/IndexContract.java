package tech.quantit.northstar.gateway.api.domain.contract;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.TickDataAware;
import tech.quantit.northstar.common.constant.ChannelType;
import tech.quantit.northstar.common.constant.Constants;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.model.Identifier;
import tech.quantit.northstar.gateway.api.domain.mktdata.IndexTicker;
import tech.quantit.northstar.gateway.api.domain.mktdata.MinuteBarGenerator;
import tech.quantit.northstar.gateway.api.domain.time.TradeTimeDefinition;
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
public class IndexContract implements Contract, TickDataAware{

	private final List<Contract> monthContracts;
	
	private final MinuteBarGenerator barGen;
	
	private final IndexTicker ticker;
	
	private final ContractField contract;
	
	private final Identifier identifier;
	
	private boolean hasSubscribed;
	
	public IndexContract(FastEventEngine feEngine, List<Contract> monthContracts) {
		this.monthContracts = monthContracts;
		this.contract = makeIndexContractField(monthContracts.get(0).contractField());
		this.identifier = Identifier.of(contract.getContractId());
		this.barGen = new MinuteBarGenerator(contract, monthContracts.get(0).tradeTimeDefinition(), bar -> feEngine.emitEvent(NorthstarEventType.BAR, bar));
		this.ticker = new IndexTicker(this, t -> {
			feEngine.emitEvent(NorthstarEventType.TICK, t);
			barGen.update(t);
		});
	}
	
	private ContractField makeIndexContractField(ContractField proto) {
		String name = proto.getName().replaceAll("\\d+", "指数");
		String fullName = proto.getFullName().replaceAll("\\d+", "指数");
		String originSymbol = proto.getSymbol();
		String symbol = originSymbol.replaceAll("\\d+", Constants.INDEX_SUFFIX);
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
	public boolean subscribe() {
		for(Contract c : monthContracts) {
			if(!c.subscribe()) {
				log.warn("[{}] 合约订阅失败", c.contractField().getUnifiedSymbol());
			}
		}
		hasSubscribed = true;
		return true;
	}

	@Override
	public boolean unsubscribe() {
		for(Contract c : monthContracts) {
			if(!c.unsubscribe()) {
				log.warn("[{}] 合约取消订阅失败", c.contractField().getUnifiedSymbol());
			}
		}
		hasSubscribed = false;
		return true;
	}
	
	@Override
	public boolean hasSubscribed() {
		return hasSubscribed;
	}

	@Override
	public void onTick(TickField tick) {
		ticker.update(tick);
	}

	@Override
	public List<Contract> memberContracts() {
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
		barGen.endOfBar();
	}
	
}
