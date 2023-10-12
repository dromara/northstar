package org.dromara.northstar.strategy.example;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.dromara.northstar.common.constant.FieldType;
import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.constant.SignalOperation;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.ListValue;
import org.dromara.northstar.common.model.NumberValue;
import org.dromara.northstar.common.model.Setting;
import org.dromara.northstar.common.model.StringValue;
import org.dromara.northstar.common.model.Value;
import org.dromara.northstar.strategy.IModuleContext;
import org.dromara.northstar.strategy.IModuleStrategyContext;
import org.dromara.northstar.strategy.StrategicComponent;
import org.dromara.northstar.strategy.TradeStrategy;
import org.dromara.northstar.strategy.constant.PriceType;
import org.dromara.northstar.strategy.model.TradeIntent;
import org.slf4j.Logger;

import com.alibaba.fastjson.JSONObject;

import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 本示例用于展示写一个策略的必要元素，以及最基本的开平仓操作、超时撤单操作
 * 
 * ## 风险提示：该策略仅作技术分享，据此交易，风险自担 ##
 * @author KevinHuangwl
 *
 */
@StrategicComponent(BeginnerExampleStrategy.NAME)		// 该注解是用于给策略命名用的，所有的策略都要带上这个注解
public class BeginnerExampleStrategy implements TradeStrategy{
	
	protected static final String NAME = "示例-简单策略";	// 之所以要这样定义一个常量，是为了方便日志输出时可以带上策略名称
	
	private InitParams params;	// 策略的参数配置信息
	
	private IModuleStrategyContext ctx;		// 模组的操作上下文
	
	private JSONObject storeObj = new JSONObject(); 	// 可透视状态计算信息
	
	private Logger log;
	
	/**
	 * 定义该策略的参数。该类每个策略必须自己重写一个，类名必须为InitParams，必须继承DynamicParams，必须是个static类。
	 * @author KevinHuangwl
	 */
	public static class InitParams extends DynamicParams {			// 每个策略都要有一个用于定义初始化参数的内部类，类名称不能改
		
		@Setting(label="操作间隔", type = FieldType.NUMBER, order = 10, unit = "秒")		// Label注解用于定义属性的元信息。可以声明单位
		private int actionInterval = 60;						// 属性可以为任意多个，当元素为多个时order值用于控制前端的显示顺序
		
		@Setting(label="锁仓演示", type = FieldType.SELECT, options = {"启用","禁用"}, optionsVal = {"true","false"}, order = 20)
		private boolean showHedge;
		
		@Setting(label="价格类型", type = FieldType.SELECT, options = {"对手价","排队价"}, optionsVal = {"OPP_PRICE", "WAITING_PRICE"}, order = 30)
		private String priceType = "OPP_PRICE";
		
	}
	
	/***************** 以下如果看不懂，基本可以照搬 *************************/
	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
		this.params = (InitParams) params;
	}
	
	@Override
	public void setContext(IModuleContext context) {
		ctx = context;
		log = ctx.getLogger();
	}
	
	@Override
	public JSONObject getStoreObject() {
		return storeObj;
	}

	@Override
	public void setStoreObject(JSONObject storeObj) {
		this.storeObj = storeObj;
	}
	/***************** 以上如果看不懂，基本可以照搬 *************************/

	private long nextActionTime;
	
	@Override
	public void onTick(TickField tick) {
		
		log.debug("TICK触发: C:{} D:{} T:{} P:{} V:{} OI:{} OID:{}", 
				tick.getUnifiedSymbol(), tick.getActionDay(), tick.getActionTime(), 
				tick.getLastPrice(), tick.getVolume(), tick.getOpenInterest(), tick.getOpenInterestDelta());
		long now = tick.getActionTimestamp();
		// 启用后，等待10秒才开始交易
		if(nextActionTime == 0) {
			nextActionTime = now + 10000;
		}
		boolean flag = ThreadLocalRandom.current().nextBoolean();
		if(now > nextActionTime) {
			nextActionTime = now + params.actionInterval * 1000;
			log.info("开始交易");
			if(ctx.getState().isEmpty()) {
				SignalOperation op = flag ? SignalOperation.BUY_OPEN : SignalOperation.SELL_OPEN;	// 随机开多或者开空
				if(ctx.getState() == ModuleState.EMPTY_HEDGE) {
					op = flag ? SignalOperation.BUY_CLOSE : SignalOperation.SELL_CLOSE;
				}
				ctx.submitOrderReq(TradeIntent.builder()
						.contract(ctx.getContract(tick.getUnifiedSymbol()))
						.operation(op)
						.volume(ctx.getDefaultVolume())
						.priceType(PriceType.valueOf(params.priceType))
						.timeout(10000)
						.build());
				return;
			}
			if(ctx.getState() == ModuleState.HOLDING_LONG) {
				SignalOperation op = params.showHedge ? SignalOperation.SELL_OPEN : SignalOperation.SELL_CLOSE;
				ctx.submitOrderReq(TradeIntent.builder()
						.contract(ctx.getContract(tick.getUnifiedSymbol()))
						.operation(op)
						.priceType(PriceType.valueOf(params.priceType))
						.volume(ctx.getDefaultVolume())
						.timeout(3000)
						.build());
			}
			if(ctx.getState() == ModuleState.HOLDING_SHORT) {		
				SignalOperation op = params.showHedge ? SignalOperation.BUY_OPEN : SignalOperation.BUY_CLOSE;
				ctx.submitOrderReq(TradeIntent.builder()
						.contract(ctx.getContract(tick.getUnifiedSymbol()))
						.operation(op)
						.priceType(PriceType.valueOf(params.priceType))
						.volume(ctx.getDefaultVolume())
						.timeout(3000)
						.build());
			}
		}
	}

	@Override
	public void onMergedBar(BarField bar) {
		log.debug("策略每分钟触发");
	}

	@Override
	public void onOrder(OrderField order) {
		// 委托单状态变动回调
	}

	@Override
	public void onTrade(TradeField trade) {
		// 成交回调
	}

	@Override
	public List<Value> strategyInfos() {
		// 用户可以通过该方法，把策略内部非指标化的计算值暴露给监控台
		// 比如可以监控放量的价位都都出现在哪些价位等，用线性指标无法很好表示的离散值
		return List.of(
				new NumberValue("示例数值", 1000.889977),
				new StringValue("示例文本", "样例数据"),
				new ListValue("示例列表", List.of(new NumberValue("", 123), new NumberValue("", 456)))
				);
	}
	
}
