package org.dromara.northstar.rl;

import static org.junit.jupiter.api.Assertions.*;

import org.dromara.northstar.ai.rl.model.RLAction;
import org.dromara.northstar.rl.env.CartPoleV0;
import org.junit.jupiter.api.Test;

class GymEnvTest {
	
	@Test
	void test() {
		assertDoesNotThrow(() -> {
			try(CartPoleV0 env = new CartPoleV0();){
				env.reset();
				env.interact(new RLAction(0));
			}
		});
	}

}
