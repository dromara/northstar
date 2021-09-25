package tech.xuanwu.northstar.strategy.cta.module.dealer;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.model.ContractManager;
import tech.xuanwu.northstar.strategy.common.Dealer;
import tech.xuanwu.northstar.strategy.common.Signal;
import tech.xuanwu.northstar.strategy.common.model.ModuleStatus;
import tech.xuanwu.northstar.strategy.cta.module.signal.CtaSignal;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;

@Slf4j
public abstract class AbstractDealer implements Dealer{

	protected CtaSignal currentSignal;
	
	protected OffsetFlagEnum currentOffset;
	
	@Setter
	protected ContractManager contractManager;
	
	protected String bindedUnifiedSymbol;
	
	protected int openVol;
	
	protected String priceTypeStr;
	
	protected int overprice;
	
	protected ModuleStatus moduleStatus;
	
	@Override
	public Set<String> bindedUnifiedSymbols() {
		return Set.of(bindedUnifiedSymbol);
	}
	
	@Override
	public void onSignal(Signal signal, OffsetFlagEnum offsetFlag) {
		currentSignal = (CtaSignal) signal;
		currentOffset = offsetFlag;
	}
	
	@Override
	public void setModuleStatus(ModuleStatus status) {
		moduleStatus = status;
	}
	
	protected double resolvePrice(CtaSignal currentSignal, TickField tick) {
		int factor = currentSignal.getState().isBuy() ? 1 : -1;
		ContractField contract = contractManager.getContract(tick.getUnifiedSymbol());
		double priceTick = contract.getPriceTick();
		double orderPrice = 0;
		switch(priceTypeStr) {
		case "对手价":
			double oppPrice = currentSignal.getState().isBuy() ? tick.getAskPrice(0) : tick.getBidPrice(0);
			orderPrice = oppPrice + factor * priceTick * overprice;
			log.info("当前使用[对手价]成交，基础价为：{}，超价：{} Tick，最终下单价：{}", oppPrice, overprice, orderPrice);
			break;
		case "市价":
			orderPrice = currentSignal.getState().isBuy() ? tick.getUpperLimit() : tick.getLowerLimit();
			log.info("当前使用[市价]成交，最终下单价：{}", orderPrice);
			break;
		case "最新价":
			orderPrice = tick.getLastPrice() + factor * priceTick * overprice;
			log.info("当前使用[最新价]成交，基础价为：{}，超价：{} Tick，最终下单价：{}", tick.getLastPrice(), overprice, orderPrice);
			break;
		case "排队价":
			orderPrice = currentSignal.getState().isBuy() ? tick.getBidPrice(0) : tick.getAskPrice(0);
			log.info("当前使用[排队价]成交，基础价为：{}，忽略超价，最终下单价：{}", orderPrice, orderPrice);
			break;
		case "信号价":
			if(!StringUtils.equals(currentSignal.getSourceUnifiedSymbol(), bindedUnifiedSymbol)) {
				log.warn("限价会根据信号价格来计算，当信号源合约与下单合约不一致时，有可能会导致下单价格异常。当前信号源合约为：{}，下单合约为：{}", 
						currentSignal.getSourceUnifiedSymbol(), bindedUnifiedSymbol);
			}
			orderPrice = currentSignal.getSignalPrice() + factor * priceTick * overprice;
			log.info("当前使用[限价]成交，基础价为：{}，超价：{} Tick，最终下单价：{}", currentSignal.getSignalPrice(), overprice, orderPrice);
			break;
		default:
			throw new IllegalStateException("未知下单价格类型：" + priceTypeStr);
		}
		return orderPrice;
	}
}
