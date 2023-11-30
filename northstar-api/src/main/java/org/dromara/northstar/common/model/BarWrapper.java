package org.dromara.northstar.common.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import xyz.redtorch.pb.CoreField.BarField;

@Deprecated
@EqualsAndHashCode
@Getter
public class BarWrapper implements Comparable<BarWrapper>{

	private BarField bar;
	private boolean unsettled;
	
	public BarWrapper(BarField bar) {
		this.bar = bar;
	}
	
	/**
	 * @param bar			K线数据
	 * @param unsettled		是否为未确定值（会发生变化）
	 */
	public BarWrapper(BarField bar, boolean unsettled) {
		this.bar = bar;
		this.unsettled = unsettled;
	}

	@Override
	public int compareTo(BarWrapper o) {
		return bar.getActionTimestamp() < o.bar.getActionTimestamp() ? -1 : 1;
	}
}
