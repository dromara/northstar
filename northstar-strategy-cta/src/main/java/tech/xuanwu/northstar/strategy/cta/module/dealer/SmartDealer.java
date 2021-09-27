package tech.xuanwu.northstar.strategy.cta.module.dealer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.utils.BarGenerator;
import tech.xuanwu.northstar.common.utils.FieldUtils;
import tech.xuanwu.northstar.strategy.common.Dealer;
import tech.xuanwu.northstar.strategy.common.Signal;
import tech.xuanwu.northstar.strategy.common.annotation.Setting;
import tech.xuanwu.northstar.strategy.common.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.common.constants.ModuleState;
import tech.xuanwu.northstar.strategy.common.constants.PriceType;
import tech.xuanwu.northstar.strategy.common.event.ModuleEventType;
import tech.xuanwu.northstar.strategy.common.model.data.SimpleBar;
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
 * 把信号作为交易基线，当行情与信号同向，并穿过基线时入场；
 * 当自行裁量时间超时后，按最新价发单，由风控策略决定是否接受入场价；
 * 当最近信号为开仓信号，且模组因智能出场条件触发已出场，当行情与信号同向，并穿过基线时入场；
 * 
 * 智能出场条件：
 * 当收到出场信号时，马上出场；
 * 当入场后一定时间内，盈利没有突破安全区，且浮盈为负，出场；
 * 当入场后盈利突破安全区，当盈亏上影达到盈亏总波幅一定比例时出场；
 * 
 * 基线变更条件：
 * 开仓信号价更新；
 * 盈亏总波幅更新；
 * 
 * @author KevinHuangwl
 *
 */
@Slf4j
@StrategicComponent("智能交易策略")
public class SmartDealer extends AbstractDealer implements Dealer {
	
	protected double baseline;
	
	//智能策略收到信号后可自行裁量的时长（秒）
	protected int signalAccordanceTimeout;
	
	protected long actionDeadline;
	
	protected int stopProfitThresholdInTick;
	
	protected int periodToleranceInDangerZoon;
	
	protected SubmitOrderReqField currentOrderReq;
	
	protected TickField lastTick;
	
	protected CtaSignal lastSignal;
	
	protected BarField lastMinBar;
	
	protected SimpleBar holdingProfitBar;
	
	protected long toleranceDeadline;
	
	private BarGenerator barGen = new BarGenerator(bindedUnifiedSymbol, (bar, minTick) -> lastMinBar = bar);
	
	private long lastSmartCloseTime;
	
	@Override
	public Optional<SubmitOrderReqField> onTick(TickField tick) {
		lastTick = tick;
		barGen.updateTick(tick);
		if(currentSignal == lastSignal || lastMinBar == null) {
			return Optional.empty();
		}
		
		log.info("{} 最新价：{}", tick.getUnifiedSymbol(), tick.getLastPrice());
		CtaSignal signal = getSignal();
		if(baseline == 0) {			
			updateBaseline(resolvePrice(signal, lastTick));
		}
		DirectionEnum direction = signal.getState().isBuy() ? DirectionEnum.D_Buy : DirectionEnum.D_Sell;
		ContractField contract = contractManager.getContract(tick.getUnifiedSymbol());
		if(moduleStatus.at(ModuleState.EMPTY)) {
			// 当模组无持仓时，等待TICK穿越基线触发市价开单
			if(triggerSmartOpening()) {
				moduleStatus.transform(ModuleEventType.OPENING_SIGNAL_CREATED);
			}
		} else if (moduleStatus.at(ModuleState.HOLDING_LONG) || moduleStatus.at(ModuleState.HOLDING_SHORT)) {
			if(triggerSmartClosingByTimeout() || triggerSmartClosingByRange()) {
				log.info("[{}] 智能触发平仓", moduleStatus.getModuleName());
				moduleStatus.transform(ModuleEventType.STOP_LOSS);
				lastSmartCloseTime = System.currentTimeMillis();
				OffsetFlagEnum offset = moduleStatus.isSameDayHolding(tick.getTradingDay()) ? OffsetFlagEnum.OF_CloseToday : OffsetFlagEnum.OF_CloseYesterday;
				DirectionEnum dir = moduleStatus.at(ModuleState.HOLDING_LONG) ? DirectionEnum.D_Sell : DirectionEnum.D_Buy;
				currentOrderReq = genSubmitOrder(contract, dir, offset, openVol, 0, 0);
				return Optional.of(currentOrderReq);
			}
		} else if (moduleStatus.at(ModuleState.PLACING_ORDER)) {
			// 自行裁量期触发
			if(triggerSmartOpening()) {
				log.info("[{}] 智能触发开仓", moduleStatus.getModuleName());
				currentOrderReq = genSubmitOrder(contract, direction, OffsetFlagEnum.OF_Open, openVol, 0, signal.getStopPrice());
				lastSignal = signal;
				currentSignal = null;
				return Optional.of(currentOrderReq);
			}
			//当价格已经触及止损时，不应该再开仓
			if(signal.isOpening() && 
					(signal.isBuy() && lastTick.getLastPrice() < signal.getStopPrice() || !signal.isBuy() && lastTick.getLastPrice() > signal.getStopPrice())) {
				log.info("[{}] 智能终止开仓", moduleStatus.getModuleName());
				moduleStatus.transform(ModuleEventType.SIGNAL_RETAINED);
				return Optional.empty();
			}
			//当模组无持仓时，等待超时后，按最新价生成订单
			if(currentSignal != null && currentSignal.isOpening() && System.currentTimeMillis() > actionDeadline) {
				log.info("[{}] 自行裁量时间结束，信号触发开仓", moduleStatus.getModuleName());
				currentOrderReq = genSubmitOrder(contract, direction, OffsetFlagEnum.OF_Open, openVol, tick.getLastPrice(), currentSignal.getStopPrice());
				lastSignal = currentSignal;
				currentSignal = null;
				return Optional.of(currentOrderReq);
			}
			// 当模组为持仓时，按信号平仓
			if(currentSignal != null && !currentSignal.isOpening()) {
				log.info("[{}] 信号触发平仓", moduleStatus.getModuleName());
				OffsetFlagEnum offset = moduleStatus.isSameDayHolding(tick.getTradingDay()) ? OffsetFlagEnum.OF_CloseToday : OffsetFlagEnum.OF_CloseYesterday;
				currentOrderReq = genSubmitOrder(contract, direction, offset, openVol, 0, 0);
				lastSignal = null;
				currentSignal = null;
				return Optional.of(currentOrderReq);
			}
		}
		
		return Optional.empty();
	}

	@Override
	public void onTrade(TradeField trade) {
		if(FieldUtils.isOpen(trade.getOffsetFlag())) {
			holdingProfitBar = new SimpleBar(0);
			toleranceDeadline = System.currentTimeMillis() + periodToleranceInDangerZoon * 1000;
		} else {
			holdingProfitBar = null;
		}
	}
	
	private boolean triggerSmartClosingByTimeout() {
		holdingProfitBar.update(moduleStatus.getHoldingProfit());
		if(System.currentTimeMillis() > toleranceDeadline && holdingProfitBar.actualDiff() < 0) {
			log.info("[{}] 限定时间内浮盈没有达到安全边际，触发智能平仓", moduleStatus.getModuleName());
			return true;
		}
		return false;
	}
	
	private boolean triggerSmartClosingByRange() {
		holdingProfitBar.update(moduleStatus.getHoldingProfit());
		ContractField contract = contractManager.getContract(lastTick.getUnifiedSymbol());
		double stopProfitThresholdInProfit = contract.getMultiplier() * openVol * stopProfitThresholdInTick;
		if(holdingProfitBar.getHigh() > stopProfitThresholdInProfit && (holdingProfitBar.upperShadow() * 1.0 / holdingProfitBar.barRange()) > 0.25) {
			log.info("[{}] 回撤幅度超过额定幅度，触发智能平仓", moduleStatus.getModuleName());
			updateBaseline(lastTick.getLastPrice());
			return true;
		}
		return false;
	}
	
	private boolean triggerSmartOpening() {
		// 180秒冷静时间，防止无序开仓
		if(lastMinBar == null || System.currentTimeMillis() - lastSmartCloseTime < 180000) {
			return false;
		}
		CtaSignal signal = getSignal();
		return (signal.isBuy() && lastMinBar.getClosePrice() < baseline && lastTick.getLastPrice() >= baseline) 
				|| (!signal.isBuy() && lastMinBar.getClosePrice() > baseline && lastTick.getLastPrice() <= baseline);
	}
	
	private CtaSignal getSignal() {
		CtaSignal signal = currentSignal != null ? currentSignal : lastSignal;
		if(signal == null) {
			throw new IllegalStateException("没有信号记录");
		}
		return signal;
	}
	
	private void updateBaseline(double val) {
		log.info("[{}] 基线值更新：[{}]->[{}]", moduleStatus.getModuleName(), baseline, val);
		baseline = val;
	}
	
	@Override
	public void onSignal(Signal signal) {
		super.onSignal(signal);
		if(signal.isOpening()) {
			baseline = 0;
			actionDeadline = System.currentTimeMillis() + signalAccordanceTimeout * 1000;
			log.info("[{}] 收到开仓信号：操作{}，价格{}，止损{}", moduleStatus.getModuleName(), ((CtaSignal)signal).getState(), signal.price(), signal.stopPrice());
			log.info("[{}] 当前价格基线已重置", moduleStatus.getModuleName());
			log.info("[{}] 自由裁量时间：{}秒", moduleStatus.getModuleName(), signalAccordanceTimeout);
			log.info("[{}] 自行裁量截止时间：{}", moduleStatus.getModuleName(), LocalDateTime.ofInstant(Instant.ofEpochMilli(actionDeadline), ZoneId.systemDefault()));
		} else {
			log.info("[{}] 收到平仓信号：操作{}，价格{}", moduleStatus.getModuleName(), ((CtaSignal)signal).getState(), signal.price());
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
		this.signalAccordanceTimeout = initParams.signalAccordanceTimeout;
		this.stopProfitThresholdInTick = initParams.stopProfitThresholdInTick;
		this.periodToleranceInDangerZoon = initParams.periodToleranceInDangerZoon;
		this.priceTypeStr = initParams.priceTypeStr;
	}

	public static class InitParams extends DynamicParams{

		@Setting(value="绑定合约", order = 10)
		private String bindedUnifiedSymbol;
		
		@Setting(value="开仓手数", order = 20)
		private int openVol;
		
		@Setting(value="自行裁量时长", order = 30, unit="秒")
		private int signalAccordanceTimeout;
		
		@Setting(value="止盈区阀值", order = 40, unit="TICK")
		private int stopProfitThresholdInTick;
		
		@Setting(value="危险区时长", order = 50, unit="秒")
		private int periodToleranceInDangerZoon;
		
		@Setting(value="基线价格类型", order = 60, options = {PriceType.OPP_PRICE, PriceType.ANY_PRICE, PriceType.LAST_PRICE, PriceType.QUEUE_PRICE, PriceType.SIGNAL_PRICE})
		private String priceTypeStr;
	}
	
}
