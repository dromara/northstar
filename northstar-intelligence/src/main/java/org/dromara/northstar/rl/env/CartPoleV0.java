package org.dromara.northstar.rl.env;

import org.dromara.northstar.rl.GymEnv;

public class CartPoleV0 extends GymEnv{

	public CartPoleV0() {
		super("CartPole-v0");
	}

	@Override
	public int terminatedScore() {
		return 250;
	}

	@Override
	public int maxEpisodes() {
		return 200;
	}

}
