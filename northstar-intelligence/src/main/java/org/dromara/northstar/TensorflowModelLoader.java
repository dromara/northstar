package org.dromara.northstar;

import java.util.Iterator;

import org.tensorflow.GraphOperation;
import org.tensorflow.Operand;
import org.tensorflow.Operation;
import org.tensorflow.Result;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.ndarray.Shape;
import org.tensorflow.types.TFloat32;

public class TensorflowModelLoader {
	
	static float[] input = new float[] {2.729812f  ,   1.958112f  ,   2.1268175f ,   2.6107285f ,
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
        	Iterator<GraphOperation> it = model.graph().operations();
        	while (it.hasNext()) {
                System.out.println(it.next().name());
            }
            // 创建会话
//            try (Session session = model.session()) {
//                
////                // 准备模型的输入数据
////                // 假设我们的模型输入是一个浮点数的二维张量（例如，1 x 64）
////                float[][] inputData = new float[1][64];
////                // ...在这里填充inputData...
////                
////                // 创建输入Tensor
////                Tensor inputTensor = Tensor.create(inputData);
////                
////                // 运行模型，获取输出
////                // "input"是输入的操作名称，"output"是输出的操作名称
////                // 这些名称取决于你的模型，可能需要修改
////                Tensor result = session.runner()
////                        .feed("input", inputTensor)
////                        .fetch("output")
////                        .run()
////                        .get(0);
//            	Tensor output = null;
//            	try(TFloat32 input = Tensor.of(TFloat32.class, Shape.of(64L))){
//            		result = session
//	            		.runner()
//	            		.feed("input", input)
//	            		.run()
//	            		.get(0);
//            	}
//                
//                // 处理模型的输出数据
//                float[][] outputData = new float[1][1]; // 假设模型的输出是 1 x 1 的
//                result.copyTo(outputData);
                
                // 使用输出数据
//                float prediction = outputData[0][0];
//                System.out.println("Prediction: " + prediction);
            	
//            }
        }
	}
}
