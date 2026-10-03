package org.dromara.northstar.common.model.rl;

import java.util.List;
import java.util.Map;

import org.dromara.northstar.common.model.ComponentField;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RL 模型视图对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RLModelVO {

	private String modelId;
	private String name;
	private String algo;
	/** 绑定的策略类全限定名 */
	private String strategyClass;
	/** 神经网络结构（创建时固化，只读展示） */
	private NetworkVO network;
	/** 超参数字段清单：由模型的 Hyperparameter 类定义，UI 据此动态渲染表单 */
	private List<ComponentField> hyperParamFields;
	/** 版本 metadata：每个版本保存一份该版本的超参数取值 */
	private List<VersionMetaVO> versionDetails;
	private int latestVersion;

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class NetworkVO {
		private List<Integer> hidden;
		private String activation;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class VersionMetaVO {
		private int version;
		private Map<String, Double> hyperParams;
	}
}
