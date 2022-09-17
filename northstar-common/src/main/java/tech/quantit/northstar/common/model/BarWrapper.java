package tech.quantit.northstar.common.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import xyz.redtorch.pb.CoreField.BarField;

@EqualsAndHashCode
@Getter
public class BarWrapper implements Comparable<BarWrapper>{

	private BarField bar;
	private boolean isSettled = true;
	
	public BarWrapper(BarField bar) {
		this.bar = bar;
	}
	
	/**
	 * @param bar			K线数据
	 * @param isSettled		是否为确定值（不会再发生变化）
	 */
	public BarWrapper(BarField bar, boolean isSettled) {
		this.bar = bar;
		this.isSettled = isSettled;
	}

	@Override
	public int compareTo(BarWrapper o) {
		return bar.getActionTimestamp() < o.bar.getActionTimestamp() ? -1 : 1;
	}
}
