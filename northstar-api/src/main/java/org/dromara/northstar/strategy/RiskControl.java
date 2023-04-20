package org.dromara.northstar.strategy;

import org.dromara.northstar.common.TickDataAware;

import xyz.redtorch.pb.CoreField.SubmitOrderReqField;

/**
 * 风控接口
 * @author KevinHuangwl
 *
 */
public interface RiskControl extends TickDataAware {
	/**
	 * 设定账户
	 * @param account
	 */
	void setAccount(IAccount account);
	/**
	 * 设定模组上下文
	 * @param ctx
	 */
	void setModuleContext(IModuleContext ctx);
	/**
	 * 风控检查
	 * @param orderReq
	 */
	void doValidateSubmitOrderReq(SubmitOrderReqField orderReq);
}
