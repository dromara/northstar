package org.dromara.northstar.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class TimeValues {

	private long timestamp;

	@JsonIgnore
	private String[] fields;
	
	private Double[] values;
}
