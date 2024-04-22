package org.dromara.northstar.ai.infer;

import java.nio.file.Path;

import org.springframework.util.Assert;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.SessionFunction;
import org.tensorflow.Signature;
import org.tensorflow.Tensor;
import org.tensorflow.SavedModelBundle.Loader;
import org.tensorflow.ndarray.NdArrays;
import org.tensorflow.ndarray.Shape;
import org.tensorflow.ndarray.buffer.FloatDataBuffer;
import org.tensorflow.ndarray.impl.buffer.raw.RawDataBufferFactory;
import org.tensorflow.types.TFloat32;


/**
 * 预训练模型
 * @auth KevinHuangwl
 */
public class PretrainedModel {
	
	private static final String TAG = "serve";
	
	private final Loader loader;
	
	private final int inputDim;
	private final int outputDim;

	public PretrainedModel(String modelName, int inputDim, int outputDim) {
		this.loader = SavedModelBundle.loader("data/" + modelName).withTags(TAG);	// 默认加载JAR包同级目录下的data目录下的模型
		this.inputDim = inputDim;
		this.outputDim = outputDim;
	}
	
	public PretrainedModel(Path path, int inputDim, int outputDim) {
		this.loader = SavedModelBundle.loader(path.toString()).withTags(TAG);
		this.inputDim = inputDim;
		this.outputDim = outputDim;
	}
	
	/**
	 * 使用预训练的模型进行预测
	 * 该方法为阻塞方法，应该在调用时进行异步处理
	 * @param inputs		需要注意这里的模型输入维度应该与训练时一致，否则会报错
	 * @return
	 */
	public float[] predict(float... inputs) {
		Assert.isTrue(inputs.length == inputDim, () -> String.format("输入层的维度与期望不一致。期望%s，实际%s", inputDim, inputs.length));
		float[] result = new float[outputDim];
		try (SavedModelBundle model = loader.load()) {
			// 获取默认的推理函数
			SessionFunction function = model.function(Signature.DEFAULT_KEY);
			FloatDataBuffer dataBuf = RawDataBufferFactory.create(inputs, true);
			try (Tensor inputTensor = TFloat32.tensorOf(NdArrays.wrap(Shape.of(1L, inputs.length), dataBuf))) {
				try (Tensor outputTensor = function.call(inputTensor)) {
					outputTensor.asRawTensor().data().asFloats().read(result);
				}
			}
		}
		return result;
	}
}
