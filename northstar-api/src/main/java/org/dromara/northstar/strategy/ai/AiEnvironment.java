package org.dromara.northstar.strategy.ai;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.utils.LocalEnvUtils;
import org.dromara.northstar.strategy.AbstractStrategy;
import org.dromara.northstar.strategy.IModuleContext;
import org.dromara.northstar.strategy.model.FeedbackDefinition;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Retrofit;

@Slf4j
public class AiEnvironment {
	
	private FeedbackDefinition feedback;
	
	private RLContextAPI proxy;
	
	private RLAware rlAware;
	
	private IModuleContext ctx;
	
	private boolean trainMode;
	
	@Setter
	private AiTrainingContext trainingCtx;
	
	public AiEnvironment(AbstractStrategy strategy) {
		Retrofit retrofit = new Retrofit.Builder()
			    .baseUrl("http://url")	//FIXME HTTP服务地址
			    .build();
		this.proxy = retrofit.create(RLContextAPI.class);
		this.ctx = strategy.getContext();
		this.rlAware = (RLAware) strategy;
		this.feedback = FeedbackDefinition.builder()
				.reward(rlAware.reward())
				.state(rlAware.state())
				.build();
		this.trainMode = StringUtils.equals(LocalEnvUtils.getEnvironment().getProperty("spring.profiles.active"), "train");
		if(trainMode) {
			log.info("当前AI处于【训练模式】");
		} else {
			log.info("当前AI处于【实用模式】");
		}
	}
	
	public void exchange() {
		try {
			if(trainMode) {
				boolean epochEnded = trainingCtx.epochEnded();
				feedback.setEndOfEpoch(epochEnded);
				proxy.train(feedback).execute();
				if(epochEnded) {
					trainingCtx.startOver();
				}
			} else {
				proxy.evaluate(feedback).execute();
			}
		} catch (IOException e) {
			ctx.getLogger().error("", e);
		}
	}

}
