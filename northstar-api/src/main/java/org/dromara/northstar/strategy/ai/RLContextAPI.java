package org.dromara.northstar.strategy.ai;

import retrofit2.Call;

public interface RLContextAPI {

	/**
	 * 创建模型/导入模型
	 * @return
	 */
	Call<Boolean> create();
	/**
	 * 保存模型
	 * @return
	 */
	Call<Boolean> save();
	/**
	 * 训练模型
	 * @return
	 */
	Call<Integer> train();
	/**
	 * 执行运算
	 * @return
	 */
	Call<Integer> evaluate();
	/**
	 * 导出模型
	 * @return
	 */
	Call<Boolean> exportModel();
	
}
