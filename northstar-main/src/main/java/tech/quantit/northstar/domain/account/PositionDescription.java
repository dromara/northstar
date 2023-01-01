package tech.quantit.northstar.domain.account;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;

import tech.quantit.northstar.common.exception.InsufficientException;
import tech.quantit.northstar.common.exception.NoSuchElementException;
import tech.quantit.northstar.common.model.Identifier;
import tech.quantit.northstar.common.model.OrderRequest;
import tech.quantit.northstar.common.utils.OrderUtils;
import tech.quantit.northstar.gateway.api.IContractManager;
import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ForceCloseReasonEnum;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;
import xyz.redtorch.pb.CoreEnum.VolumeConditionEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;

/**
 * 持仓信息描述
 * @author KevinHuangwl
 *
 */
public class PositionDescription {

	/**
	 * 数据结构
	 * symbol:	{ [0]空头持仓， [1]多头持仓 }
	 */
	protected ConcurrentHashMap<String, PositionField[]> posMap = new ConcurrentHashMap<>();
	
	protected IContractManager contractMgr;
	
	public PositionDescription(IContractManager contractMgr) {
		this.contractMgr = contractMgr;
	}
	
	/**
	 * 更新持仓
	 * @param pos
	 */
	public void update(PositionField pos) {
		String contractId = pos.getContract().getContractId();
		posMap.computeIfAbsent(contractId, id -> new PositionField[2]);
		if(pos.getPositionDirection() == PositionDirectionEnum.PD_Long) {
			posMap.get(contractId)[1] = pos;
		} else if(pos.getPositionDirection() == PositionDirectionEnum.PD_Short) {
			posMap.get(contractId)[0] = pos;
		}
	}
	
	private PositionField acquireTargetPosition(OrderRequest orderReq) {
		if(!OrderUtils.isClosingOrder(orderReq.getTradeOpr())) {
			throw new IllegalStateException("该委托并非平仓委托");
		}
		DirectionEnum orderDir = OrderUtils.resolveDirection(orderReq.getTradeOpr());
		PositionDirectionEnum targetPosDir = OrderUtils.getClosingDirection(orderDir);
		int i = targetPosDir == PositionDirectionEnum.PD_Long ? 1 : targetPosDir == PositionDirectionEnum.PD_Short ? 0 : -1;
		if(!posMap.containsKey(orderReq.getContractId()) || posMap.get(orderReq.getContractId())[i] == null) {
			throw new NoSuchElementException("找不到可平仓的持仓");
		}
		return posMap.get(orderReq.getContractId())[i];
	}
	
	/**
	 * 生成平仓请求
	 * @param orderReq
	 * @return
	 * @throws InsufficientException 
	 */
	public List<SubmitOrderReqField> generateCloseOrderReq(OrderRequest orderReq) throws InsufficientException{
		ContractField contract = contractMgr.getContract(Identifier.of(orderReq.getContractId())).contractField();
		if(contract == null) {
			throw new NoSuchElementException("不存在此合约：" + orderReq.getContractId());
		}
		PositionField pos = acquireTargetPosition(orderReq);
		int totalAvailable = pos.getPosition() - pos.getFrozen();
		int tdAvailable = pos.getTdPosition() - pos.getTdFrozen();
		if(totalAvailable < orderReq.getVolume()) {
			throw new InsufficientException("持仓不足，无法平仓");
		}
		DirectionEnum orderDir = OrderUtils.resolveDirection(orderReq.getTradeOpr());
		List<SubmitOrderReqField> result = new ArrayList<>();
		double price = Double.parseDouble(orderReq.getPrice());
		SubmitOrderReqField.Builder sb = SubmitOrderReqField.newBuilder();
		sb.setContract(contract)
		.setOriginOrderId(UUID.randomUUID().toString())
		.setPrice(price)
		.setStopPrice(StringUtils.isNotBlank(orderReq.getStopPrice()) ? Double.parseDouble(orderReq.getStopPrice()) : 0D)
		.setOrderPriceType(price <= 0 ? OrderPriceTypeEnum.OPT_AnyPrice : OrderPriceTypeEnum.OPT_LimitPrice)
		.setDirection(orderDir)
		.setHedgeFlag(HedgeFlagEnum.HF_Speculation)
		.setTimeCondition(TimeConditionEnum.TC_GFD)
		.setVolumeCondition(VolumeConditionEnum.VC_AV)
		.setForceCloseReason(ForceCloseReasonEnum.FCR_NotForceClose)
		.setContingentCondition(ContingentConditionEnum.CC_Immediately)
		.setMinVolume(1)
		.setGatewayId(orderReq.getGatewayId());
		
		if(pos.getContract().getExchange() == ExchangeEnum.SHFE && tdAvailable > 0) {
			if(tdAvailable >= orderReq.getVolume()) {
				result.add(sb.setVolume(orderReq.getVolume())
						.setOffsetFlag(OffsetFlagEnum.OF_CloseToday)
						.build());
				return result;
			}
			
			result.add(sb.setVolume(tdAvailable)
					.setOffsetFlag(OffsetFlagEnum.OF_CloseToday)
					.build());
			result.add(sb.setVolume(orderReq.getVolume() - tdAvailable)
					.setOffsetFlag(OffsetFlagEnum.OF_CloseYesterday)
					.build());
			return result;
		}
		
		result.add(sb.setVolume(orderReq.getVolume())
				.setOffsetFlag(OffsetFlagEnum.OF_Close)
				.build());
		return result;
	}
	
	public List<PositionField> getPositions(){
		List<PositionField> result = new ArrayList<>(posMap.size() * 2);
		posMap.forEach((k,v) -> {
			for(PositionField p : v) {
				if(p != null) {
					result.add(p);
				}
			}
		});
		return Collections.unmodifiableList(result);
	}
}
