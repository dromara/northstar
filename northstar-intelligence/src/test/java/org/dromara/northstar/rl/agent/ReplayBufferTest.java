package org.dromara.northstar.rl.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.dromara.northstar.ai.rl.model.RLAction;
import org.dromara.northstar.ai.rl.model.RLExperience;
import org.dromara.northstar.ai.rl.model.RLReward;
import org.dromara.northstar.ai.rl.model.RLState;
import org.junit.jupiter.api.Test;

class ReplayBufferTest {

	ReplayBuffer buf = new ReplayBuffer(10);
	
	@Test
	void testAddAndSize() {
		assertThat(buf.size()).isZero();
		for(int i=0; i<20; i++) {
			buf.add(new RLExperience(new RLState(-i), new RLAction(1), new RLReward(i), new RLState(i), false));
		}
		assertThat(buf.size()).isEqualTo(10);
	}
	
	@Test
	void testSample() {
		testAddAndSize();
		
		assertThat(buf.sample(5)).hasSize(5);
	}
	
	@Test
	void testSampleError() {
		testAddAndSize();
		
		assertThrows(IllegalArgumentException.class, () -> {
			buf.sample(100);
		});
	}

}
