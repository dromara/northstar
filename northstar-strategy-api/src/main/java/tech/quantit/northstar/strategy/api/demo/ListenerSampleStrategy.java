package tech.quantit.northstar.strategy.api.demo;

import java.util.Optional;
import java.util.TimerTask;
import java.util.concurrent.ThreadLocalRandom;

import com.alibaba.fastjson.JSONObject;

import tech.quantit.northstar.common.constant.ModuleState;
import tech.quantit.northstar.common.constant.SignalOperation;
import tech.quantit.northstar.common.model.DynamicParams;
import tech.quantit.northstar.common.utils.FieldUtils;
import tech.quantit.northstar.strategy.api.AbstractStrategy;
import tech.quantit.northstar.strategy.api.IModuleContext;
import tech.quantit.northstar.strategy.api.TradeStrategy;
import tech.quantit.northstar.strategy.api.annotation.StrategicComponent;
import tech.quantit.northstar.strategy.api.constant.DisposablePriceListenerType;
import tech.quantit.northstar.strategy.api.constant.PriceType;
import tech.quantit.northstar.strategy.api.utils.time.TickBasedTimer;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 本示例用于展示一个带止盈止损的策略
 * 
 * ## 风险提示：该策略仅作技术分享，据此交易，风险自担 ##
 * @author KevinHuangwl
 *
 */
@StrategicComponent(ListenerSampleStrategy.NAME)		// 该注解是用于给策略命名用的，所有的策略都要带上这个注解
public class ListenerSampleStrategy extends AbstractStrategy implements TradeStrategy{
	
	protected static final String NAME = "示例-止盈止损策略";	// 之所以要这样定义一个常量，是为了方便日志输出时可以带上策略名称
	
	/**
	 * 定义该策略的参数。该类每个策略必须自己重写一个，类名必须为InitParams，必须继承DynamicParams，必须是个static类。
	 * @author KevinHuangwl
	 */
	public static class InitParams extends DynamicParams {			// 每个策略都要有一个用于定义初始化参数的内部类，类名称不能改
		// 策略的配置参数可以为
	}
	
	/***************** 以下如果看不懂，基本可以照搬 *************************/
	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
		// 由于本策略没有配置参数，所以没有参数赋值
	}
	
	@Override
	public void setContext(IModuleContext context) {
		ctx = context;
		log = ctx.getLogger();
	}
	
	@Override
	public JSONObject getComputedState() {
		return inspectableState;
	}

	@Override
	public void setComputedState(JSONObject stateObj) {
		this.inspectableState = stateObj;
	}
	/***************** 以上如果看不懂，基本可以照搬 *************************/
	
	private TickBasedTimer timer = new TickBasedTimer();
	private TimerTask runningTask = null;
	private TimerTask withdrawOrderIfTimeout = new TimerTask() {
		@Override
		public void run() {
			originOrderId.ifPresent(ctx::cancelOrder);
		}
	};
	
	@Override
	public void onOrder(OrderField order) {
		if(runningTask == null && ctx.getState().isWaiting()) {
			runningTask = withdrawOrderIfTimeout;
			timer.schedule(runningTask, 5000);		// 5秒超时撤单
		}
	}

	@Override
	public void onTrade(TradeField trade) {
		if(trade.getOriginOrderId().equals(originOrderId.get())) {
			runningTask = null;
		}
		if(FieldUtils.isOpen(trade.getOffsetFlag())) {
			ctx.priceTriggerOut(trade, DisposablePriceListenerType.TAKE_PROFIT, TICK_EARN);		// 设置止盈 	
			ctx.priceTriggerOut(trade, DisposablePriceListenerType.STOP_LOSS, TICK_STOP);		// 设置止损
		}
	}
	
	private long nextActionTime;
	private Optional<String> originOrderId = Optional.empty();
	
	private static final int TICK_STOP = -5;		// 五个价位止损
	private static final int TICK_EARN = 10;	// 十个价位止盈

	@Override
	public void onTick(TickField tick) {
		timer.onTick(tick);
		
		long now = tick.getActionTimestamp();
		// 启用后，等待10秒才开始交易
		if(nextActionTime == 0) {
			nextActionTime = now + 10000;
		}
		
		boolean flag = ThreadLocalRandom.current().nextBoolean();
		if(now > nextActionTime && ctx.getState() == ModuleState.EMPTY) {
			ContractField contract = ctx.getContract(tick.getUnifiedSymbol());
			// 开仓方向
			SignalOperation openOpr = flag ? SignalOperation.BUY_OPEN : SignalOperation.SELL_OPEN;
			originOrderId = ctx.submitOrderReq(contract, openOpr, PriceType.WAITING_PRICE, 1, tick.getLastPrice());
		}
	}

}
