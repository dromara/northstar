package org.dromara.northstar;

import java.util.concurrent.ThreadLocalRandom;

import org.tensorflow.ConcreteFunction;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.SessionFunction;
import org.tensorflow.Signature;
import org.tensorflow.Tensor;
import org.tensorflow.ndarray.FloatNdArray;
import org.tensorflow.ndarray.NdArrays;
import org.tensorflow.ndarray.Shape;
import org.tensorflow.ndarray.StdArrays;
import org.tensorflow.ndarray.buffer.FloatDataBuffer;
import org.tensorflow.ndarray.impl.buffer.raw.RawDataBufferFactory;
import org.tensorflow.ndarray.impl.dense.FloatDenseNdArray;
import org.tensorflow.op.core.Placeholder;
import org.tensorflow.types.TFloat32;

public class TensorflowModelLoader {
	
	static float[] inputData = new float[] {2.729812f  ,   1.958112f  ,   2.1268175f ,   2.6107285f ,
	         4.1623726f ,   2.9390707f ,  -1.1724995f ,   0.2440019f ,
	         5.7644386f ,   4.4159517f ,   1.6132894f ,   1.0913455f ,
	         1.7029684f ,  -3.8716962f ,  -3.0412815f ,  -5.487889f ,
	       -15.718265f  , -20.818876f  , -23.77352f   , -22.181406f  ,
	       -43.184532f  , -49.06372f   , -50.634964f  , -47.316006f  ,
	         0.8791688f ,   0.57590705f,   0.27575433f,   0.39804783f,
	        -0.30877325f,  -0.5321218f ,  -0.6448755f ,  -0.75189877f,
	        -0.3370133f ,  -0.3115597f ,  -0.31398815f,  -0.16238794f,
	         1.0585665f ,   1.1945051f ,   1.4704733f ,   1.7134079f ,
	         1.6581562f ,   1.6142559f ,   1.5310798f ,   1.1555663f ,
	        -0.2198281f ,  -0.27489725f,  -0.3114017f ,  -0.3291053f ,
	         0.19931138f,   0.22424221f,   0.26744872f,   0.32045272f,
	         0.25419554f,   0.2830821f ,   0.30978668f,   0.33669493f,
	        -0.94445217f,  -1.0498289f ,  -1.1681589f ,  -1.3130333f ,
	        -3.3061903f ,  -3.4965053f ,  -3.6902695f ,  -3.8852913f};

	public static void main(String[] args) {
		// 加载模型
        try (SavedModelBundle model = SavedModelBundle.load("C:\\Users\\KevinHuangwl\\northstar\\model", "serve")) {
        	// 获取默认的推理函数
           SessionFunction function = model.function(Signature.DEFAULT_KEY);

            // 准备输入数据，这里我们假设是一个浮点数的二维数组
            FloatDataBuffer dataBuf = RawDataBufferFactory.create(inputData, true);
            // 创建输入Tensor
            try (Tensor inputTensor = TFloat32.tensorOf(NdArrays.wrap(Shape.of(1L, 64L), dataBuf))) {
                // 运行模型并获取输出
                try (Tensor outputTensor = function.call(inputTensor)) {
                    // 获取输出数据
                    // 打印输出结果，或者进行进一步的处理
                    System.out.println("Model output: " + outputTensor.asRawTensor().data().asFloats().getFloat(0));
                }
            }
    		
        }
	}
}
