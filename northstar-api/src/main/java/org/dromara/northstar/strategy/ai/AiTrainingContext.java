package org.dromara.northstar.strategy.ai;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


public class AiTrainingContext {

	
	
	public void startOver() {
		
	}
	
	public boolean epochEnded() {
		return false;
	}

}
