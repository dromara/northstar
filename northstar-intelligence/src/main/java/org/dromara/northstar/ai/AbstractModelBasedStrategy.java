package org.dromara.northstar.ai;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import org.dromara.northstar.ai.infer.PretrainedModel;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.strategy.AbstractStrategy;
import org.slf4j.Logger;

/**
 * 实现了AI能力的抽象策略
 * @auth KevinHuangwl
 */
public abstract class AbstractModelBasedStrategy extends AbstractStrategy implements SamplingAware {
	
	protected static final String MODE_PREDICTING = "predicting";
	protected static final String MODE_SAMPLING = "sampling";
	
	protected PretrainedModel model;
	
	private Logger logger;
	
	private ExecutorService exec = CommonUtils.newThreadPerTaskExecutor(getClass());
	
	protected AbstractModelBasedStrategy() {
		logger = ctx.getLogger(getClass());
		// 处于非采样阶段时，需要检查预训练模型是否存在
		if(!isSampling()) {
			logger.info("准备从models目录加载预训练模型");
			Path path = Paths.get("models/" + getContext().getModule().getName());
			if(!path.toFile().exists()) {
				throw new IllegalStateException("在models目录下找不到此模组的预训练模型");
			}
			model = new PretrainedModel(path, inputDim(), outputDim());
		}
	}

	/**
	 * 神经网络输入层维度
	 * @return
	 */
	protected abstract int inputDim();
	/**
	 * 神经网络输出层维度
	 * @return
	 */
	protected abstract int outputDim();
	
	/**
	 * 预测
	 * 注意，由于model.predict是阻塞方法，因此应该使用异步回调来处理计算结果
	 * @return
	 */
	protected CompletableFuture<float[]> predict(){
		return CompletableFuture.supplyAsync(() -> model.predict(sample().states()), exec);
	}
	
}
