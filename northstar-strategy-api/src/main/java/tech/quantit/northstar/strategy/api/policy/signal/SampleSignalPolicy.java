package tech.quantit.northstar.strategy.api.policy.signal;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import tech.quantit.northstar.strategy.api.AbstractSignalPolicy;
import tech.quantit.northstar.strategy.api.SignalPolicy;
import tech.quantit.northstar.strategy.api.annotation.Setting;
import tech.quantit.northstar.strategy.api.annotation.StrategicComponent;
import tech.quantit.northstar.strategy.api.constant.ModuleState;
import tech.quantit.northstar.strategy.api.constant.SignalOperation;
import tech.quantit.northstar.strategy.api.model.DynamicParams;
import tech.quantit.northstar.strategy.api.model.TimeSeriesValue;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 本示例用于展示写一个策略的必要元素
 * 
 * ## 风险提示：该策略仅作技术分享，据此交易，风险自担 ##
 * @author KevinHuangwl
 *
 */
@StrategicComponent("示例信号策略")	// 该注解是用于给策略命名用的
public class SampleSignalPolicy extends AbstractSignalPolicy
	implements SignalPolicy //	所有的策略都应该是DynamicParamsAware的实现类
{
	private int actionInterval;
	
	private long nextActionTime = -1;
	
	/**
	 * 获取策略的动态参数对象
	 */
	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	/**
	 * 策略的所有参数初始化逻辑
	 */
	@Override
	public void initWithParams(DynamicParams params) {
		InitParams initParams = (InitParams) params;
		this.bindedUnifiedSymbol = initParams.bindedUnifiedSymbol;
		this.actionInterval = initParams.actionInterval;
		this.numOfRefData = initParams.numOfRefData;
		this.periodMins = initParams.periodMins;
	}
	
	/**
	 * 定义该策略的参数，类名必须为InitParams，必须继承DynamicParams，必须是个static类
	 * @author KevinHuangwl
	 */
	public static class InitParams extends DynamicParams{
		
		@Setting(value="绑定合约", order=10)	// Label注解用于定义属性的元信息
		private String bindedUnifiedSymbol;		// 属性可以为任意多个，当元素为多个时order值用于控制前端的显示顺序
		
		@Setting(value="周期时长", order=11, unit="分钟")
		private int periodMins;
		
		@Setting(value="回溯周期数", order=12)
		private int numOfRefData;
		
		@Setting(value="操作间隔", order=20, unit="秒")	// 可以声明单位
		private int actionInterval;
		
	}
	
	@Override
	public String name() {
		return "示例信号策略";
	}

	/**
	 * 数据初始化入口
	 */
	@Override
	public void initByTick(Iterable<TickField> ticks) {
		// 示例代码不需要初始化数据
	}

	/**
	 * 数据初始化入口
	 */
	@Override
	public void initByBar(Iterable<BarField> bars) {
		// 示例代码不需要初始化数据
	}

	int seed = ThreadLocalRandom.current().nextInt(10);
	/**
	 * 策略逻辑驱动入口
	 * 每个TICK触发
	 */
	@Override
	protected void handleTick(TickField tick) {
		log.debug("策略每个TICK触发: {} {} {}", tick.getUnifiedSymbol(), tick.getActionTime(), tick.getLastPrice());
		long now = tick.getActionTimestamp();
		//初始状态下，等待10秒才开始交易
		if(nextActionTime < 0) {
			nextActionTime = now + 10000;
		}
		if(now > nextActionTime) {
			nextActionTime = now + actionInterval * 1000;
			log.info("开始交易");
			if(currentState == ModuleState.EMPTY) {
				SignalOperation op = (++seed & 1) > 0 ? SignalOperation.BUY_OPEN : SignalOperation.SELL_OPEN;
				emit(op, 0, 5);	// 假设固定止损为5个价位
			}
			if(currentState == ModuleState.HOLDING_LONG) {	
				emit(SignalOperation.SELL_CLOSE);
			}
			if(currentState == ModuleState.HOLDING_SHORT) {			
				emit(SignalOperation.BUY_CLOSE);
			}
		}
	}

	/**
	 * 策略逻辑驱动入口
	 * 每分钟触发
	 */
	@Override
	protected void handleBar(BarField bar) {
		log.debug("策略每分钟触发");
	}

	@Override
	public Map<String, TimeSeriesValue[]> inspectRefData() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
