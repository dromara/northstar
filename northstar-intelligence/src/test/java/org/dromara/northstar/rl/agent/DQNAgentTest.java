package org.dromara.northstar.rl.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.dromara.northstar.ai.rl.model.RLAction;
import org.dromara.northstar.ai.rl.model.RLExperience;
import org.dromara.northstar.ai.rl.model.RLReward;
import org.dromara.northstar.ai.rl.model.RLState;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DQNAgentTest {
	
	static DQNAgent agent;
	
	@BeforeAll
	static void startup() throws Exception {
		agent = new DQNAgent();
	}
	
	@AfterAll
	static void cleanup() throws IOException {
		agent.close();
	}

	@Test
	void testReact() {
		assertThat(agent.react(new RLState(0D, 0D, 0D, 0D))).isInstanceOf(RLAction.class);
	}

	@Test
	void testLearn() {
		assertDoesNotThrow(() -> {
			RLState state = new RLState(0D, 0D, 0D, 0D);
			RLAction action = new RLAction(0);
			RLReward reward = new RLReward(0);
			agent.learn(new RLExperience(state, action, reward, state, false));
		});
	}
	
	@Test
	void testUpdate() {
		assertDoesNotThrow(() -> {
			agent.update();
		});
	}
	
	@Test
	void testSave() {
		assertDoesNotThrow(() -> {
			agent.save("test");
		});
	}
	
	@Test
	void testLoad() {
		assertDoesNotThrow(() -> {
			agent.load("test2");
		});
	}
	
}
