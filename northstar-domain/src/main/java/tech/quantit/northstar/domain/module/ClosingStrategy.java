package tech.quantit.northstar.domain.module;

import tech.quantit.northstar.common.constant.SignalOperation;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;

public interface ClosingStrategy {

	/**
	 * 解析操作
	 * @param opr
	 * @param accStore
	 * @return
	 */
	OffsetFlagEnum resolveOperation(SignalOperation opr, IModuleAccountStore accStore);
}
