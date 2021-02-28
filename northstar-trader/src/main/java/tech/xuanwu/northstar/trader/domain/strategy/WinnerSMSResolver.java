package tech.xuanwu.northstar.trader.domain.strategy;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.ForceCloseReasonEnum;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;
import xyz.redtorch.pb.CoreEnum.VolumeConditionEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;

public class WinnerSMSResolver {

	private static final Pattern PTN = Pattern.compile("【[^】]+】\\s*赢家\\d+号：(?<symbol>[A-z0-9]+)(?:在(?<closePrice>[0-9\\.]+)的价格平(?<closeDir>.)单，)?(?:在(?<openPrice>[0-9\\.]+)的价格开(?<openDir>.)单，止损价：(?<stopPrice>[0-9\\.]+)，)?目前[^，]+，仅供参考。");
	
	private Map<String, ContractField> contractMap;
	
	public WinnerSMSResolver(Map<String, ContractField> contractMap) {
		this.contractMap = contractMap;
	}
	
	public SubmitOrderReqField[] resolve(String msg) {
		Matcher m = PTN.matcher(msg);
		if(!m.matches()) {
			throw new IllegalArgumentException("不符合规格的短信信息：" + msg);
		}
		
		String symbolStr = m.group("symbol");
		String closePriceStr = m.group("closePrice"); 
		String closeDirStr = m.group("closeDir");
		String openPriceStr = m.group("openPrice");
		String openDirStr = m.group("openDir");
		String stopPriceStr = m.group("stopPrice");
		
		ContractField contract = contractMap.get(symbolStr);
		if(contract == null) {
			throw new IllegalStateException("没有找到相应的合约信息：" + symbolStr);
		}
		SubmitOrderReqField closeReq = null;
		SubmitOrderReqField openReq = null;
		
		if(StringUtils.isNotEmpty(closeDirStr)) {
			closeReq = SubmitOrderReqField.newBuilder()
					.setContract(contract)
					.setPrice(Double.parseDouble(closePriceStr))
					.setVolume(1)
					.setDirection(StringUtils.equals("多", closeDirStr) ? DirectionEnum.D_Sell : StringUtils.equals("空", closeDirStr) ? DirectionEnum.D_Buy : DirectionEnum.D_Unknown)
					.setOffsetFlag(OffsetFlagEnum.OF_ForceClose)
					.setOrderPriceType(OrderPriceTypeEnum.OPT_LimitPrice)
					.setHedgeFlag(HedgeFlagEnum.HF_Speculation)
					.setTimeCondition(TimeConditionEnum.TC_GFD)
					.setVolumeCondition(VolumeConditionEnum.VC_AV)
					.setForceCloseReason(ForceCloseReasonEnum.FCR_Other)
					.setContingentCondition(ContingentConditionEnum.CC_Immediately)
					.setMinVolume(1)
					.build();
		}
		if(StringUtils.isNotEmpty(openDirStr)) {
			openReq = SubmitOrderReqField.newBuilder()
					.setContract(contract)
					.setPrice(Double.parseDouble(openPriceStr))
					.setVolume(1)
					.setDirection(StringUtils.equals("多", openDirStr) ? DirectionEnum.D_Buy : StringUtils.equals("空", openDirStr) ? DirectionEnum.D_Sell : DirectionEnum.D_Unknown)
					.setOffsetFlag(OffsetFlagEnum.OF_Open)
					.setOrderPriceType(OrderPriceTypeEnum.OPT_LimitPrice)
					.setHedgeFlag(HedgeFlagEnum.HF_Speculation)
					.setTimeCondition(TimeConditionEnum.TC_GFD)
					.setVolumeCondition(VolumeConditionEnum.VC_AV)
					.setForceCloseReason(ForceCloseReasonEnum.FCR_NotForceClose)
					.setContingentCondition(ContingentConditionEnum.CC_Immediately)
					.setStopPrice(Double.parseDouble(stopPriceStr))
					.setMinVolume(1)
					.build();
		}
		
		if(closeReq != null && openReq != null) {
			return new SubmitOrderReqField[] {closeReq, openReq};
		} else if(closeReq != null) {
			return new SubmitOrderReqField[] {closeReq};
		} else if(openReq != null) {
			return new SubmitOrderReqField[] {openReq};
		}
		
		throw new IllegalStateException("非法规格信息：" + msg);
	}
	
}
