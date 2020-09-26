package tech.xuanwu.northstar.strategy.config.common;

import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;
import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;
import xyz.redtorch.pb.CoreEnum.VolumeConditionEnum;

@Getter
@Setter
@Component
public class DefaultSubmitOrderConfig {

	/**
	 * 委托时效。默认立即成交，否则撤单（该委托策略可以避免自己实现撤单逻辑）
	 */
	private TimeConditionEnum timeCondition = TimeConditionEnum.TC_IOC;
	
	/**
	 * 投机套保标志。默认投机
	 */
	private HedgeFlagEnum hedgeFlag = HedgeFlagEnum.HF_Speculation;
	
	/**
	 * 委托单类型。默认限价单
	 */
	private OrderPriceTypeEnum orderPriceType = OrderPriceTypeEnum.OPT_LimitPrice;
	
	/**
	 * 成交量类型。默认任意
	 */
	private VolumeConditionEnum volumeCondition = VolumeConditionEnum.VC_AV;
	
	/**
	 * 触发类型。默认立即触发
	 */
	private ContingentConditionEnum trigerCondition = ContingentConditionEnum.CC_Immediately;
}
