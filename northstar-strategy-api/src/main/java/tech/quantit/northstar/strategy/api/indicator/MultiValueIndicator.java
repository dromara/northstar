package tech.quantit.northstar.strategy.api.indicator;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import tech.quantit.northstar.common.exception.NoSuchElementException;
import tech.quantit.northstar.strategy.api.BarDataAware;
import xyz.redtorch.pb.CoreField.BarField;

/**
 * 多值指标抽象类
 * @author KevinHuangwl
 *
 */
public abstract class MultiValueIndicator implements BarDataAware {

	private Map<String, Indicator> indicatorMap = new HashMap<>();
	
	private String unifiedSymbol;
	
	/**
	 * 获取指标回溯值
	 * @param name				指标值名称
	 * @param numOfStepBack		回溯步长。0代表当前值，正数代表从近到远N个周期前的值，负数代表从远到近N个周期的值
	 * 例如有一个记录集 [77,75,80,99]， 当前指针是2，即值为80。
	 * 当回溯步长为0时，返回80；
	 * 当回溯步长为1时，返回75；
	 * 当回溯步长为2时，返回77；
	 * 当回溯步长为3时，返回99；
	 * 当回溯步长为-1时，返回99；
	 * 如此类推
	 * @return
	 */
	public double value(String name, int numOfStepBack) {
		checkExist(name);
		return indicatorMap.get(name).value(numOfStepBack);
	}
	
	/**
	 * 获取指标回溯值 
	 * @param time		指标值对应的时间戳
	 * @return
	 */
	public Optional<Double> valueOn(String name, long time){
		checkExist(name);
		return indicatorMap.get(name).valueOn(time); 
	}
	
	/**
	 * 更新值 
	 */
	@Override
	public void onBar(BarField bar) {
		for(Map.Entry<String, Indicator> e : indicatorMap.entrySet()) {
			e.getValue().onBar(bar);
		}
	}
	
	/**
	 * 指标绑定合约
	 * @return
	 */
	public String bindedUnifiedSymbol() {
		return unifiedSymbol;
	}
	
	/**
	 * 指标是否已完成初始化
	 * @return
	 */
	public boolean isReady() {
		return indicatorMap.entrySet().stream()
				.filter(e -> !e.getValue().isReady())
				.toList()
				.isEmpty();
	}
	
	protected void setIndicator(String name, Indicator indicator) {
		unifiedSymbol = indicator.bindedUnifiedSymbol();
		indicatorMap.put(name, indicator);
	}
	
	private void checkExist(String name) {
		if(!indicatorMap.containsKey(name)) {
			throw new NoSuchElementException("不存在 [" + name + "] 指标值");
		}
	}
	
}
