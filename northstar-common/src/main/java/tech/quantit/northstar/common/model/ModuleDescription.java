package tech.quantit.northstar.common.model;

import lombok.Builder;
import tech.quantit.northstar.common.constant.ModuleState;

/**
 * 模组信息
 * @author KevinHuangwl
 *
 */
@Builder
public record ModuleDescription (
		/**
		 * 模组名称
		 */
		String moduleName,
		/**
		 * 模组启用状态
		 */
		boolean enabled,
		/***
		 * 模组状态
		 */
		ModuleState moduleState,
		/**
		 * 模组账户描述
		 */
		ModuleAccountDescription accountDescription,
		/**
		 * 模组持仓描述
		 */
		ModulePositionDescription positionDescription,
		/**
		 * 模组账户描述2（套利套保场景可能需要多个账户）
		 */
		ModuleAccountDescription extAccountDescription,
		/**
		 * 模组持仓描述2（套利套保场景可能需要多个账户）
		 */
		ModulePositionDescription extPositionDescription
		){}
