package org.dromara.northstar.strategy.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ConditionalOnProperty(prefix = "spring.profiles", name = "active", havingValue = "train")
@Order(Ordered.LOWEST_PRECEDENCE)
@Component
public class AiTrainingRunner implements CommandLineRunner{
	
	@Autowired
	AiTrainingContext trainingCtx;

	@Override
	public void run(String... args) throws Exception {
		new Thread(() -> {
			log.info("AI模型训练准备开始");
			try {
				Thread.sleep(5000);
				trainingCtx.startOver();
			} catch (InterruptedException e) {
				log.warn("", e);
			}
			
		}).start();
	}

}
