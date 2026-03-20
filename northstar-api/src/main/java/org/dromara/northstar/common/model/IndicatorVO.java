package org.dromara.northstar.common.model;

import org.dromara.northstar.common.constant.LineStyle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class IndicatorVO {

	private String name;
	
	private LineStyle style;
	
}
