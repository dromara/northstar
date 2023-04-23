package org.dromara.northstar.demo.strategy;

import java.util.concurrent.ThreadLocalRandom;

import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.constant.SignalOperation;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.utils.FieldUtils;
import org.dromara.northstar.strategy.AbstractStrategy;
import org.dromara.northstar.strategy.IModuleContext;
import org.dromara.northstar.strategy.StrategicComponent;
import org.dromara.northstar.strategy.TradeStrategy;
import org.dromara.northstar.strategy.constant.DisposablePriceListenerType;
import org.dromara.northstar.strategy.constant.PriceType;
import org.dromara.northstar.strategy.model.TradeIntent;

import com.alibaba.fastjson.JSONObject;

import xyz.redtorch.pb.CoreField.ContractField;
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
	
	@Override
	public void onTrade(TradeField trade) {
		if(FieldUtils.isOpen(trade.getOffsetFlag())) {
			ctx.priceTriggerOut(trade, DisposablePriceListenerType.TAKE_PROFIT, TICK_EARN);		// 设置止盈 	
			ctx.priceTriggerOut(trade, DisposablePriceListenerType.STOP_LOSS, TICK_STOP);		// 设置止损
		}
	}
	
	private long nextActionTime;
	
	private static final int TICK_STOP = -5;		// 五个价位止损
	private static final int TICK_EARN = 10;	// 十个价位止盈

	@Override
	public void onTick(TickField tick) {
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
			ctx.submitOrderReq(TradeIntent.builder()
					.contract(contract)
					.operation(openOpr)
					.priceType(PriceType.WAITING_PRICE)
					.volume(1)
					.timeout(5000)
					.build());
		}
	}

}
