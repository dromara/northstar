package org.dromara.northstar.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.redtorch.pb.CoreEnum.BarCycleEnum;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class BarVO {

	private long timestamp;
	
	private double open;
	
	private double close;
	
	private double high;
	
	private double low;
	
	private double volume;
	
	private long oi;
	
	private String tradingDay;
	
	private BarCycleEnum period; // 周期分钟
	
}
