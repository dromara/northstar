package tech.xuanwu.northstar.strategy.cta.module.dealer;

import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.strategy.common.Dealer;
import tech.xuanwu.northstar.strategy.common.annotation.Setting;
import tech.xuanwu.northstar.strategy.common.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.common.model.meta.DynamicParams;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

@Slf4j
@StrategicComponent("示例交易策略")
public class SampleDealer extends AbstractDealer implements Dealer {
	
	private SubmitOrderReqField currentOrderReq;

	//注意防止重复下单
	@Override
	public Optional<SubmitOrderReqField> onTick(TickField tick) {
		if(currentSignal == null && currentOrderReq == null) {
			return Optional.empty();
		}
		if(currentSignal != null) {
			DirectionEnum direction = currentSignal.getState().isBuy() ? DirectionEnum.D_Buy : DirectionEnum.D_Sell;
			ContractField contract = contractManager.getContract(tick.getUnifiedSymbol());
			OffsetFlagEnum offset;
			if(currentSignal.getState().isOpen()) {
				offset = OffsetFlagEnum.OF_Open;
			} else {
				offset = moduleStatus.isSameDayHolding(tick.getTradingDay()) ? OffsetFlagEnum.OF_CloseToday : OffsetFlagEnum.OF_CloseYesterday;
			}
			// 按信号下单
			currentOrderReq = genSubmitOrder(contract, direction, offset, openVol, resolvePrice(currentSignal, tick), currentSignal.getStopPrice());
			currentSignal = null;
			log.info("交易策略生成订单,订单号[{}]", currentOrderReq.getOriginOrderId());
			return Optional.of(currentOrderReq);
			
		} else {
			int factor = currentOrderReq.getDirection() == DirectionEnum.D_Buy ? 1 : -1;
			ContractField contract = contractManager.getContract(tick.getUnifiedSymbol());
			double priceTick = contract.getPriceTick();
			// 按前订单改价
			currentOrderReq = SubmitOrderReqField.newBuilder(currentOrderReq)
					.setOriginOrderId(UUID.randomUUID().toString())
					.setPrice(tick.getLastPrice() + factor * priceTick * overprice)
					.build();
			log.info("交易策略改价追单，订单号[{}]", currentOrderReq.getOriginOrderId());
			return Optional.of(currentOrderReq);
		}
	}
	
	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
		InitParams initParams = (InitParams) params;
		this.bindedUnifiedSymbol = initParams.bindedUnifiedSymbol;
		this.openVol = initParams.openVol;
		this.priceTypeStr = initParams.priceTypeStr;
		this.overprice = initParams.overprice;
	}
	
	public static class InitParams extends DynamicParams{

		@Setting(value="绑定合约", order = 10)
		private String bindedUnifiedSymbol;
		
		@Setting(value="开仓手数", order = 20)
		private int openVol;
		
		@Setting(value="价格类型", order = 30, options = {"对手价", "市价", "最新价", "排队价", "信号价"})
		private String priceTypeStr;
		
		@Setting(value="超价", order = 40, unit = "Tick")
		private int overprice;
	}

	@Override
	public void onTrade(TradeField trade) {
		if(currentOrderReq != null && StringUtils.equals(trade.getOriginOrderId(), currentOrderReq.getOriginOrderId())) {
			currentOrderReq = null;
			log.info("交易完成，订单号[{}]", trade.getOriginOrderId());
		}
	}

}
