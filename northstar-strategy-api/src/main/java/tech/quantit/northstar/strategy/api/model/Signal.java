package tech.quantit.northstar.strategy.api.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import tech.quantit.northstar.strategy.api.constant.SignalOperation;

@Getter
@AllArgsConstructor
public class Signal {

	private SignalOperation signalOperation;
	
	private double signalPrice;
	
	private int ticksToStop;
}
