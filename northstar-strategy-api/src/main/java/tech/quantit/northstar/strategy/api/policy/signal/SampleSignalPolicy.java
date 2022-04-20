package tech.quantit.northstar.strategy.api.policy.signal;

import java.util.concurrent.ThreadLocalRandom;

import tech.quantit.northstar.common.constant.ModuleState;
import tech.quantit.northstar.common.constant.SignalOperation;
import tech.quantit.northstar.strategy.api.AbstractSignalPolicy;
import tech.quantit.northstar.strategy.api.SignalPolicy;
import tech.quantit.northstar.strategy.api.annotation.Setting;
import tech.quantit.northstar.strategy.api.annotation.StrategicComponent;
import tech.quantit.northstar.strategy.api.model.DynamicParams;
import tech.quantit.northstar.strategy.api.model.Signal;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 本示例用于展示写一个策略的必要元素
 * 
 * ## 风险提示：该策略仅作技术分享，据此交易，风险自担 ##
 * @author KevinHuangwl
 *
 */
@StrategicComponent("示例信号策略")		// 该注解是用于给策略命名用的，所有的策略都要带上这个注解
public class SampleSignalPolicy 
	extends AbstractSignalPolicy	// 所有信号策略都要继承这个抽象类
	implements SignalPolicy 		// 所有信号策略都要实现这个接口
{
	private int actionInterval;
	
	private long nextActionTime = -1;
	
	/**
	 * 获取策略的动态参数对象
	 */
	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();	// 这个用于返回定义好的策略初始化参数
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
	public static class InitParams extends DynamicParams {			// 每个策略都要有一个用于定义初始化参数的内部类，类名称不能改
		
		@Setting(value="绑定合约", order=10)		// Label注解用于定义属性的元信息
		private String bindedUnifiedSymbol;		// 属性可以为任意多个，当元素为多个时order值用于控制前端的显示顺序
		
		@Setting(value="周期时长", order=11, unit="分钟")
		private int periodMins;
		
		@Setting(value="回溯周期数", order=12)
		private int numOfRefData;
		
		@Setting(value="操作间隔", order=20, unit="秒")		// 可以声明单位
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
	public void initByTick(TickField ticks) {
		// 示例代码不需要初始化数据
	}

	/**
	 * 数据初始化入口
	 */
	@Override
	public void initByBar(BarField bars) {
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
				emit(Signal.builder().signalOperation(op).ticksToStop(5).build(), tick.getActionTimestamp());	// 假设固定止损为5个价位
			}
			if(currentState == ModuleState.HOLDING_LONG) {	
				emit(Signal.builder().signalOperation(SignalOperation.SELL_CLOSE).build(), tick.getActionTimestamp());
			}
			if(currentState == ModuleState.HOLDING_SHORT) {			
				emit(Signal.builder().signalOperation(SignalOperation.BUY_CLOSE).build(), tick.getActionTimestamp());
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
	public boolean hasDoneInit() {
		// 这里可以强制规定初始化的条件
		return true;
	}
	
}
