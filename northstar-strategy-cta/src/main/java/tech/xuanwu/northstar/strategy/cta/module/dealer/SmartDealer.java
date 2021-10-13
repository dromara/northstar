package tech.xuanwu.northstar.strategy.cta.module.dealer;

import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.utils.BarGenerator;
import tech.xuanwu.northstar.strategy.common.Dealer;
import tech.xuanwu.northstar.strategy.common.Signal;
import tech.xuanwu.northstar.strategy.common.annotation.Setting;
import tech.xuanwu.northstar.strategy.common.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.common.constants.Bool;
import tech.xuanwu.northstar.strategy.common.constants.ModuleState;
import tech.xuanwu.northstar.strategy.common.constants.PriceType;
import tech.xuanwu.northstar.strategy.common.event.ModuleEventType;
import tech.xuanwu.northstar.strategy.common.model.meta.DynamicParams;
import tech.xuanwu.northstar.strategy.cta.module.signal.CtaSignal;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 智能交易策略不以交易信号作为下单操作的驱动事件，
 * 交易信号仅仅作为入场基线
 * 本策略默认采用最新价计算
 * 
 * 智能入场条件：
 * 空仓时，且基线大于零，当行情穿过基线时入场；
 * 
 * 智能出场条件：
 * 在开仓观察期内，当行情达到止损位则出场；
 * 若持仓为多头，当价格小于等于基线，则出场；
 * 若持仓为空头，当价格大于等于基线，则出场；
 * 
 * 基线变更条件：
 * 开仓信号更新；
 * 
 * ## 风险提示：该策略仅作技术分享，据此交易，风险自担 ##
 * 
 * @author KevinHuangwl
 *
 */
@Slf4j
@StrategicComponent("智能交易策略")
public class SmartDealer extends AbstractDealer implements Dealer {
	
	protected double baseline;
	
	protected SubmitOrderReqField currentOrderReq;
	
	protected int coldDownPeriod;
	
	protected int lossToleranceInTick;
	
	protected int stopLossInTick;
	
	protected TickField lastTick;
	
	protected BarField lastMinBar;
	
	protected int barNumOfLastAction;
	
	private boolean showTick;
	
	private BarGenerator barGen = new BarGenerator(bindedUnifiedSymbol, (bar, minTick) -> {
		lastMinBar = bar;
		numOfBarsForCurrentDay++;
	});
	
	@Override
	public Optional<SubmitOrderReqField> handleTick(TickField tick) {
		lastTick = tick;
		barGen.updateTick(tick);
		if(baseline == 0 || lastMinBar == null) {
			if(showTick && isPrintoutWindow() && lastMinBar == null) {
				log.warn("[{}] 正在预热，估计时间1分钟", moduleStatus.getModuleName());
			}
			if(showTick && isPrintoutWindow() && baseline == 0) {
				log.warn("[{}] 未设置基线水平");
			}
			return Optional.empty();
		}
		
		if(showTick) {			
			log.info("[{}] {} 最新价：{}", moduleStatus.getModuleName(), tick.getUnifiedSymbol(), tick.getLastPrice());
		}
		
		ContractField contract = contractManager.getContract(tick.getUnifiedSymbol());
		if(ifTriggeredLossTolerance() || triggerSmartClose()) {
			// 满足出场条件
			if(!moduleStatus.getCurrentState().isHolding()) {
				throw new IllegalStateException("模组持仓状态异常，无法平仓：" + moduleStatus.getCurrentState());
			}
			OffsetFlagEnum offset = moduleStatus.isSameDayHolding(tick.getTradingDay()) ? OffsetFlagEnum.OF_CloseToday : OffsetFlagEnum.OF_CloseYesterday;
			DirectionEnum dir = moduleStatus.at(ModuleState.HOLDING_LONG) ? DirectionEnum.D_Sell : DirectionEnum.D_Buy;
			currentOrderReq = genSubmitOrder(contract, dir, offset, openVol, 0, 0);
			moduleStatus.transform(ModuleEventType.STOP_LOSS);
			return Optional.of(currentOrderReq);
		}
		
		if(triggerSmartBuyOpen()) {
			moduleStatus.transform(ModuleEventType.OPENING_SIGNAL_CREATED);
			double stopLossPrice = lastTick.getBidPrice(0) - stopLossInTick * contract.getPriceTick();
			return Optional.of(genSubmitOrder(contract, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open, openVol, 0, stopLossPrice));
		}
		
		if(triggerSmartSellOpen()) {
			moduleStatus.transform(ModuleEventType.OPENING_SIGNAL_CREATED);
			double stopLossPrice = lastTick.getAskPrice(0) + stopLossInTick * contract.getPriceTick();
			return Optional.of(genSubmitOrder(contract, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Open, openVol, 0, stopLossPrice));
		}
		
		return Optional.empty();
	}
	
	//防止大量打印日志，故限制打印的时间窗口，每分钟可以打印两秒
	private boolean isPrintoutWindow() {
		return (System.currentTimeMillis() % 60000) < 2000; 
	}

	@Override
	public void onTrade(TradeField trade) {
		barNumOfLastAction = numOfBarsForCurrentDay;
	}
	
	private boolean ifTriggeredLossTolerance() {
		int factor = moduleStatus.at(ModuleState.HOLDING_LONG) ? 1 : moduleStatus.at(ModuleState.HOLDING_SHORT) ? -1 : 0;
		ContractField contract = contractManager.getContract(lastTick.getUnifiedSymbol());
		if(moduleStatus.getCurrentState().isHolding() && withinColdDownPeriod() 
				&& factor * (baseline - lastTick.getLastPrice()) > lossToleranceInTick * contract.getPriceTick()) {
			log.info("[{}] 观察期触发止损", moduleStatus.getModuleName());
			return true;
		}
		return false;
	}
	
	private boolean withinColdDownPeriod() {
		return numOfBarsForCurrentDay - barNumOfLastAction <= coldDownPeriod;
	}
	
	private boolean triggerSmartClose() {
		if(withinColdDownPeriod() || !moduleStatus.getCurrentState().isHolding()) {
			return false;
		}
		if(moduleStatus.at(ModuleState.HOLDING_LONG) && lastTick.getLastPrice() <= baseline
				|| moduleStatus.at(ModuleState.HOLDING_SHORT) && lastTick.getLastPrice() >= baseline) {
			log.info("[{}] 基线触发平仓", moduleStatus.getModuleName());
			return true;
		}
		return false;
	}
	
	private boolean triggerSmartBuyOpen() {
		if(moduleStatus.at(ModuleState.EMPTY) && lastMinBar.getClosePrice() < baseline 
				&& lastMinBar.getOpenPrice() < baseline && lastTick.getLastPrice() >= baseline) {
			log.info("[{}] 基线触发开多仓", moduleStatus.getModuleName());
			return true;
		}
		return false;
	}
	
	private boolean triggerSmartSellOpen() {
		if(moduleStatus.at(ModuleState.EMPTY) && lastMinBar.getClosePrice() > baseline 
				&& lastMinBar.getOpenPrice() > baseline && lastTick.getLastPrice() <= baseline) {
			log.info("[{}] 基线触发开空仓", moduleStatus.getModuleName());
			return true;
		}
		return false;
	}
	
	private void updateBaseline(double val) {
		log.info("[{}] 基线值更新：[{}]->[{}]", moduleStatus.getModuleName(), baseline, val);
		baseline = val;
	}
	
	@Override
	public void onSignal(Signal signal) {
		currentSignal = (CtaSignal) signal;
		log.info("[{}] 收到信号：价格{}", moduleStatus.getModuleName(), signal.price());
		updateBaseline(resolvePrice((CtaSignal) signal, lastTick));
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
		this.coldDownPeriod = initParams.coldDownPeriod;
		this.lossToleranceInTick = initParams.lossToleranceInTick;
		this.stopLossInTick = initParams.stopLossInTick;
		this.priceTypeStr = initParams.priceTypeStr;
		this.showTick = initParams.showTick;
	}

	public static class InitParams extends DynamicParams{

		@Setting(value="绑定合约", order = 10)
		private String bindedUnifiedSymbol;
		
		@Setting(value="开仓手数", order = 20)
		private int openVol;
		
		@Setting(value="开仓容错区", order = 40, unit="TICK")
		private int lossToleranceInTick;
		
		@Setting(value="止损", order = 45, unit="TICK")
		private int stopLossInTick;
		
		@Setting(value="开仓观察期", order = 50, unit="分钟")
		private int coldDownPeriod;
		
		@Setting(value="基线价格类型", order = 60, options = {PriceType.OPP_PRICE, PriceType.ANY_PRICE, PriceType.LAST_PRICE, PriceType.QUEUE_PRICE, PriceType.SIGNAL_PRICE})
		private String priceTypeStr;
		
		@Setting(value="打印行情日志", order = 100, options = {Bool.TRUE, Bool.FALSE})
		private boolean showTick;
	}
	
}
