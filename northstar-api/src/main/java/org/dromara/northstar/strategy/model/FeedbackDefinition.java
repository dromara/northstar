package org.dromara.northstar.strategy.model;

import org.dromara.northstar.strategy.ai.Reward;
import org.dromara.northstar.strategy.ai.State;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FeedbackDefinition {

	private Reward reward;
	
	private State state;
	
	private boolean endOfEpoch;
}
