package org.dromara.northstar.rl;

import org.dromara.northstar.ai.rl.RLAgent;
import org.dromara.northstar.ai.rl.model.RLAction;
import org.dromara.northstar.ai.rl.model.RLEnvResponse;
import org.dromara.northstar.ai.rl.model.RLExperience;
import org.dromara.northstar.ai.rl.model.RLState;
import org.dromara.northstar.rl.agent.DQNAgent;
import org.dromara.northstar.rl.env.CartPoleV0;

import lombok.extern.slf4j.Slf4j;

/**
 * 该测试仅用于开发环境下，验证强化学习模型代码是否正确
 * 当分数收敛于期望最大值时，则代表模型编码正确
 * @auth KevinHuangwl
 */
@Slf4j
public class GymEnvTester {
	
	static int winCount;
	
	public static void main(String... args) throws Exception {
		log.info("进行GYM环境测试");
		GymEnv env = new CartPoleV0();	// 可自行替换
		RLAgent agent = new DQNAgent(); // 可自行替换
		
		for(int i=0; i<env.maxEpisodes(); i++) {
			RLState state = env.reset();
			double accScore = 0;
			while(accScore < env.terminatedScore()) {
				RLAction action = agent.react(state);
				RLEnvResponse resp = env.interact(action);
				agent.learn(new RLExperience(state, action, resp.reward(), resp.state(), resp.hasDone()));
				accScore += resp.reward().value();
				if(resp.hasDone()) {
					break;
				}
			}
			agent.update();
			log.info("第{}回合，累计得分：{}", i+1, accScore);
			if(accScore >= env.terminatedScore()) {
				winCount++;
				if(winCount == 3) {
					// 连续三次达最高分则中止测试
					break;
				}
			} else {
				winCount = 0;
			}
		}
		
		agent.close();	// 关闭agent
		env.close();	// 关闭Gym环境
	}

}
