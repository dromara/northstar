package tech.xuanwu.northstar.strategy.cta.module.signal;

import java.time.LocalTime;

import tech.xuanwu.northstar.strategy.common.SignalPolicy;
import tech.xuanwu.northstar.strategy.common.annotation.Label;
import tech.xuanwu.northstar.strategy.common.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.common.model.meta.DynamicParams;

/**
 * 本示例用于展示写一个策略的必要元素
 * 注意：定义完一个类后，需要注册成spring bean。因为项目依赖了spring bean机制来管理，而不是直接的类扫描
 * @author KevinHuangwl
 *
 */
@StrategicComponent("示例策略")	// 该注解是用于给策略命名用的
public class SampleSignalPolicy extends AbstractSignalPolicy
	implements SignalPolicy //	所有的策略都应该是DynamicParamsAware的实现类
{
	
	private int shortPeriod;
	
	private int longPeriod;
	

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
		this.bindedUnifiedSymbol = initParams.unifiedSymbol;
		this.longPeriod = initParams.longPeriod;
		this.shortPeriod = initParams.shortPeriod;
	}
	
	/**
	 * 定义该策略的参数，类名必须为InitParams，必须继承DynamicParams，必须是个static类
	 * @author KevinHuangwl
	 */
	public static class InitParams extends DynamicParams{
		
		@Label(value="绑定合约", order=10)	// Label注解用于定义属性的元信息
		private String unifiedSymbol;		// 属性可以为任意多个，当元素为多个时order值用于控制前端的显示顺序
		
		@Label(value="短周期", order=20, unit="天")	// 可以声明单位
		private int shortPeriod;
		
		@Label(value="长周期", order=30, unit="天")
		private int longPeriod;

	}

	
	/**********************************************************************************/
	/**            多数情况下，要么选择onTick作为入口，要么选择onMin作为入口                **/
	/**            当两个入口同时使用的话，要谨慎处理，以免重复计算			                 **/
	/**********************************************************************************/
	/**
	 * 策略逻辑驱动入口
	 */
	@Override
	protected void onTick(int millicSecOfMin) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * 策略逻辑驱动入口
	 */
	@Override
	protected void onMin(LocalTime time) {
		// TODO Auto-generated method stub
		
	}
	
}
