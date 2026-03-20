package org.dromara.northstar.common.model;

import java.util.List;

import com.alibaba.fastjson2.JSON;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 代表一个合约的图表数据
 */
@Builder
@NoArgsConstructor
@Data
@Slf4j
public class ContractChartDescription {
	
	private ContractSimpleInfo contract;

	private String[] fields;

	private List<TimeValues> timedValues;
	
	private List<IndicatorVO> indicators;

	public ContractChartDescription(ContractSimpleInfo contract, String[] fields, List<TimeValues> timedValues, List<IndicatorVO> indicators) {
		if(timedValues == null || fields == null){
			throw new IllegalArgumentException("不能为空");
		}
		for (TimeValues timedValue : timedValues) {
			Double[] values = timedValue.getValues();
			if (fields.length != values.length){
				log.warn("字段数组：{}", JSON.toJSONString(fields));
				log.warn("值数组：{}", JSON.toJSONString(values));
				throw new IllegalArgumentException(String.format("长度不匹配: 值数组[%d]，字段数组[%d]", values.length, fields.length));
			}
		}
		this.fields = fields;
		this.timedValues = timedValues;
		this.contract = contract;
		this.indicators = indicators;
	}
}
