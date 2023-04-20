package org.dromara.northstar.demo.strategy;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.constant.FieldType;
import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.constant.SignalOperation;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.Setting;
import org.dromara.northstar.strategy.AbstractStrategy;
import org.dromara.northstar.strategy.StrategicComponent;
import org.dromara.northstar.strategy.TradeStrategy;
import org.dromara.northstar.strategy.constant.PriceType;

import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 本示例用于展示一个多合约对单账户交易的策略
 * 为简化示例，本策略的演示一个远近月价差套利策略，假设近月价格高于远月，当超过价差（近月-远月）最大值时空近月多远月；当超过价差最小值时多近月空远月；
 * 
 * ## 风险提示：该策略仅作技术分享，据此交易，风险自担 ##
 * @author KevinHuangwl
 *
 */
@StrategicComponent(ManyContractToOneAccountSampleStrategy.NAME)
public class ManyContractToOneAccountSampleStrategy extends AbstractStrategy	// 为了简化代码，引入一个通用的基础抽象类
	implements TradeStrategy{

	protected static final String NAME = "示例-多合约单账户策略";	// 之所以要这样定义一个常量，是为了方便日志输出时可以带上策略
	
	private InitParams params;	// 策略的参数配置信息
	
	private TickField lastNearbyTick;

	@Override
	protected void initMultiContractHandler() {
		// 定义近月处理逻辑
		addTickHandler(params.nearbyContract, tick -> {
			lastNearbyTick = tick;
		});
		
		// 定义远月处理逻辑
		addTickHandler(params.distantContract, tick -> {
			if(lastNearbyTick == null) {
				return;
			}
			if(ctx.getState() == ModuleState.EMPTY) {
				if(lastNearbyTick.getBidPrice(0) - tick.getAskPrice(0) > params.maxDiff) 
					ctx.submitOrderReq(ctx.getContract(tick.getUnifiedSymbol()), SignalOperation.BUY_OPEN, PriceType.ANY_PRICE, 1, 0);
				if(lastNearbyTick.getAskPrice(0) - tick.getBidPrice(0) < params.minDiff) 
					ctx.submitOrderReq(ctx.getContract(tick.getUnifiedSymbol()), SignalOperation.SELL_OPEN, PriceType.ANY_PRICE, 1, 0);
				return;
			}
			if(ctx.getState() == ModuleState.HOLDING_HEDGE) {
				if(lastNearbyTick.getBidPrice(0) - tick.getAskPrice(0) < (params.maxDiff + params.minDiff) / 2.0)
					ctx.submitOrderReq(ctx.getContract(tick.getUnifiedSymbol()), SignalOperation.BUY_CLOSE, PriceType.ANY_PRICE, 1, 0);
				if(lastNearbyTick.getAskPrice(0) - tick.getBidPrice(0) > (params.maxDiff + params.minDiff) / 2.0) 
					ctx.submitOrderReq(ctx.getContract(tick.getUnifiedSymbol()), SignalOperation.SELL_CLOSE, PriceType.ANY_PRICE, 1, 0);
			}
		});
	}
	
	@Override
	public void onTrade(TradeField trade) {
		if(StringUtils.equals(params.distantContract, trade.getContract().getUnifiedSymbol())) {
			if(trade.getOffsetFlag() == OffsetFlagEnum.OF_Open) {
				// 远月开仓成功，近月跟进
				switch(trade.getDirection()) {
				case D_Buy -> ctx.submitOrderReq(ctx.getContract(params.nearbyContract), SignalOperation.SELL_OPEN, PriceType.ANY_PRICE, 1, 0);
				case D_Sell -> ctx.submitOrderReq(ctx.getContract(params.nearbyContract), SignalOperation.BUY_OPEN, PriceType.ANY_PRICE, 1, 0);
				default -> {/* 其他情况不处理 */}
				}
			} else {
				// 远月平仓成功，近月跟进
				switch(trade.getDirection()) {
				case D_Buy -> ctx.submitOrderReq(ctx.getContract(params.nearbyContract), SignalOperation.SELL_CLOSE, PriceType.ANY_PRICE, 1, 0);
				case D_Sell -> ctx.submitOrderReq(ctx.getContract(params.nearbyContract), SignalOperation.BUY_CLOSE, PriceType.ANY_PRICE, 1, 0);
				default -> {/* 其他情况不处理 */}
				}
			}
		}
	}

	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
		this.params = (InitParams) params;
	}
	
	public static class InitParams extends DynamicParams {			
		
		@Setting(label="近月合约", order=10)		
		private String nearbyContract;
		
		@Setting(label="远月合约", order=20)
		private String distantContract;
		
		@Setting(label="最小价差", type = FieldType.NUMBER, order=30)
		private int minDiff;
		
		@Setting(label="最大价差", type = FieldType.NUMBER, order=40)
		private int maxDiff;
		
	}
}
