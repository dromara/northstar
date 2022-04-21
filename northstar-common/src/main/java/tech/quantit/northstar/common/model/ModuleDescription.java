package tech.quantit.northstar.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.quantit.northstar.common.constant.ModuleState;

/**
 * 模组信息
 * @author KevinHuangwl
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleDescription {
		/**
		 * 模组名称
		 */
		private String moduleName;
		/**
		 * 模组启用状态
		 */
		private boolean enabled;
		/***
		 * 模组状态
		 */
		private ModuleState moduleState;
		/**
		 * 模组账户描述
		 */
		private ModuleAccountDescription accountDescription;
		/**
		 * 模组持仓描述
		 */
		private ModulePositionDescription positionDescription;
		/**
		 * 模组账户描述2（套利套保场景可能需要多个账户）
		 */
		private ModuleAccountDescription extAccountDescription;
		/**
		 * 模组持仓描述2（套利套保场景可能需要多个账户）
		 */
		private ModulePositionDescription extPositionDescription;
}
