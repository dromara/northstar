package tech.quantit.northstar.strategy.api.model;

import lombok.Builder;
import lombok.Getter;
import tech.quantit.northstar.common.constant.SignalOperation;

@Getter
@Builder
public class Signal {

	private SignalOperation signalOperation;
	
	private double signalPrice;
	
	private int ticksToStop;
	
	private int volume;
}
