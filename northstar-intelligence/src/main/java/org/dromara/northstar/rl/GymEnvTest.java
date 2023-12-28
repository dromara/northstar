package org.dromara.northstar.rl;

import org.dromara.northstar.ai.rl.model.RLAction;
import org.dromara.northstar.ai.rl.model.RLEnvResponse;
import org.dromara.northstar.ai.rl.model.RLState;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("test-agent")
public class GymEnvTest implements CommandLineRunner{
	
	@Override
	public void run(String... args) throws Exception {
		log.info("进行GYM环境测试");
		// 启动PYTHON GYM 环境
		GymEnv env = new GymEnv();
		
		RLState state = env.reset();
		log.info("{}", state);
		RLEnvResponse resp = env.interact(new RLAction(1));
		log.info("{}", resp);
		
		// 关闭PYTHON GYM 环境
		env.close();
	}

}
