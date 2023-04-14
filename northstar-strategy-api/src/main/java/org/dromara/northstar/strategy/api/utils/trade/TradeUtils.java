package org.dromara.northstar.strategy.api.utils.trade;

import org.dromara.northstar.strategy.api.IModuleStrategyContext;

import tech.quantit.northstar.common.utils.FieldUtils;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 交易计算工具类
 * 用户可以在扩展项目自定义任意工具类，用于封装常用计算逻辑
 * @author KevinHuangwl
 *
 */
public class TradeUtils {

	/**
	 * 按价差与当前价计算价位
	 * @param ctx				模组上下文
	 * @param tick				当前价信息
	 * @param priceTickDiff		价差
	 * @param dir				计算方向。买方正数价差代表当前价上方；卖方正数价差代表当前价下方
	 * @return
	 */
	public static double priceAsPriceTickDiff(IModuleStrategyContext ctx, TickField tick, int priceTickDiff, DirectionEnum dir) {
		ContractField contract = ctx.getContract(tick.getUnifiedSymbol());
		int factor = FieldUtils.directionFactor(dir);
		return tick.getLastPrice() + factor * priceTickDiff * contract.getPriceTick();
	}
}
