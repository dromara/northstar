package tech.xuanwu.northstar.strategy.cta.module.dealer;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.strategy.api.AbstractDealerPolicy;
import tech.xuanwu.northstar.strategy.api.DealerPolicy;
import tech.xuanwu.northstar.strategy.api.annotation.Setting;
import tech.xuanwu.northstar.strategy.api.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.api.model.DynamicParams;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
/**
 * 
 * ## 风险提示：该策略仅作技术分享，据此交易，风险自担 ##
 * @author KevinHuangwl
 *
 */
@Slf4j
@StrategicComponent("示例交易策略")
public class SampleDealer extends AbstractDealerPolicy implements DealerPolicy {
	
	private SubmitOrderReqField currentOrderReq;

//	//注意防止重复下单
//	@Override
//	public Optional<SubmitOrderReqField> handleTick(TickField tick) {
//		if(currentSignal == null && currentOrderReq == null) {
//			return Optional.empty();
//		}
//		if(currentSignal != null) {
//			DirectionEnum direction = currentSignal.getState().isBuy() ? DirectionEnum.D_Buy : DirectionEnum.D_Sell;
//			ContractField contract = contractManager.getContract(tick.getUnifiedSymbol());
//			OffsetFlagEnum offset;
//			if(currentSignal.getState().isOpen()) {
//				offset = OffsetFlagEnum.OF_Open;
//			} else {
//				offset = moduleStatus.isSameDayHolding(tick.getTradingDay()) ? OffsetFlagEnum.OF_CloseToday : OffsetFlagEnum.OF_CloseYesterday;
//			}
//			// 按信号下单
//			currentOrderReq = genSubmitOrder(contract, direction, offset, openVol, resolvePrice(currentSignal, tick), currentSignal.getStopPrice());
//			currentSignal = null;
//			log.info("交易策略生成订单,订单号[{}]", currentOrderReq.getOriginOrderId());
//			return Optional.of(currentOrderReq);
//			
//		} else if(moduleStatus.at(ModuleState.PLACING_ORDER)) {
//			int factor = currentOrderReq.getDirection() == DirectionEnum.D_Buy ? 1 : -1;
//			ContractField contract = contractManager.getContract(tick.getUnifiedSymbol());
//			double priceTick = contract.getPriceTick();
//			// 按前订单改价
//			currentOrderReq = SubmitOrderReqField.newBuilder(currentOrderReq)
//					.setOriginOrderId(UUID.randomUUID().toString())
//					.setPrice(tick.getLastPrice() + factor * priceTick * overprice)
//					.build();
//			log.info("交易策略改价追单，订单号[{}]", currentOrderReq.getOriginOrderId());
//			return Optional.of(currentOrderReq);
//		}
//		return Optional.empty();
//	}
	
	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
		InitParams initParams = (InitParams) params;
		this.bindedUnifiedSymbol = initParams.bindedUnifiedSymbol;
//		this.openVol = initParams.openVol;
//		this.openPriceTypeStr = initParams.openPriceTypeStr;
//		this.closePriceTypeStr = initParams.closePriceTypeStr;
//		this.overprice = initParams.overprice;
	}
	
	public static class InitParams extends DynamicParams{

		@Setting(value="绑定合约", order = 10)
		private String bindedUnifiedSymbol;
		
		@Setting(value="开仓手数", order = 20)
		private int openVol;
		
		@Setting(value="开仓价格类型", order = 30, options = {"市价","对手价","最新价","排队价","信号价"})
		private String openPriceTypeStr;
		
		@Setting(value="平仓价格类型", order = 31, options = {"市价","对手价","最新价","排队价","信号价"})
		private String closePriceTypeStr;
		
		@Setting(value="超价", order = 40, unit = "Tick")
		private int overprice;
	}

	@Override
	public String name() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected SubmitOrderReqField genOrderReq(DirectionEnum direction, OffsetFlagEnum offsetFlag) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected SubmitOrderReqField genTracingOrderReq(SubmitOrderReqField originOrderReq) {
		// TODO Auto-generated method stub
		return null;
	}

}
