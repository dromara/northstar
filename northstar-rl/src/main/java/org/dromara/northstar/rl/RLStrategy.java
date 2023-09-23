package org.dromara.northstar.rl;

public interface RLStrategy {

	Reward getReward();
	
	State getState();
}
