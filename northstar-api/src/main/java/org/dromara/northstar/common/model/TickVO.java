package org.dromara.northstar.common.model;

import org.dromara.northstar.common.constant.ChannelType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class TickVO {

	private String name;
	
	private String symbol;
	
	private ChannelType channel;
	
	private double lastPrice;
	
	private double priceChange;
	
	private double volume;
	
	private long openInterest;
	
	private int openInterestDelta;
	
	private int precision;
	
	private long timestamp;
}
