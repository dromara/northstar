package org.dromara.northstar.strategy;

import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.common.model.core.SubmitOrderReq;

/**
 * 委托请求过滤器
 * 用于过滤风险，例如因程序漏洞导致连续发单，该过滤器可以作为发送订单前的最后一道安全屏障
 * @author KevinHuangwl
 *
 */
public interface OrderRequestFilter extends TickDataAware {
	/**
	 * 风控过滤
	 * @param orderReq
	 */
	void doFilter(SubmitOrderReq orderReq);
}
