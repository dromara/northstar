package org.dromara.northstar.web.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.account.AccountManager;
import org.dromara.northstar.account.TradeAccount;
import org.dromara.northstar.common.exception.InsufficientException;
import org.dromara.northstar.common.exception.NoSuchElementException;
import org.dromara.northstar.common.exception.TradeException;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.OrderRecall;
import org.dromara.northstar.common.model.OrderRequest;
import org.dromara.northstar.common.utils.OrderUtils;
import org.dromara.northstar.gateway.IContractManager;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.ForceCloseReasonEnum;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;
import xyz.redtorch.pb.CoreEnum.VolumeConditionEnum;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;

/**
 * 账户服务
 * @author KevinHuangwl
 *
 */
@Slf4j
public class AccountService {
	
	protected AccountManager accountMgr;
	
	protected IContractManager contractMgr;
	
	public AccountService(AccountManager accountMgr, IContractManager contractMgr) {
		this.accountMgr = accountMgr;
		this.contractMgr = contractMgr;
	}
	
	/**
	 * 下单
	 * @return
	 * @throws InsufficientException 
	 */
	public boolean submitOrder(OrderRequest req) throws InsufficientException {
		log.info("下单委托");
		String gatewayId = req.getGatewayId();
		TradeAccount account = accountMgr.get(Identifier.of(gatewayId));
		if(account == null) {
			throw new NoSuchElementException("没有找到该账户实例：" + gatewayId);
		}
		if(OrderUtils.isOpenningOrder(req.getTradeOpr())) {			
			account.submitOrder(generateOpenOrder(req));
		} else if(OrderUtils.isClosingOrder(req.getTradeOpr())) {
			generateCloseOrder(req).forEach(account::submitOrder);
		}
		
		return true;
	}
	
	private SubmitOrderReqField generateOpenOrder(OrderRequest orderReq) {
		ContractField contract = contractMgr.getContract(Identifier.of(orderReq.getContractId())).contractField();
		if(contract == null) {
			throw new NoSuchElementException("不存在此合约：" + orderReq.getContractId());
		}
		// 检查可用资金
		double marginRate = OrderUtils.resolveDirection(orderReq.getTradeOpr()) == DirectionEnum.D_Buy ? contract.getLongMarginRatio() : contract.getShortMarginRatio();
		double totalCost = orderReq.getVolume() * Double.parseDouble(orderReq.getPrice()) * contract.getMultiplier() * marginRate;
		TradeAccount account = accountMgr.get(Identifier.of(orderReq.getGatewayId()));
		if(account.availableAmount() < totalCost) {
			throw new InsufficientException("可用资金不足，无法开仓");
		}
		DirectionEnum orderDir = OrderUtils.resolveDirection(orderReq.getTradeOpr());
		double price = Double.parseDouble(orderReq.getPrice());
		return SubmitOrderReqField.newBuilder()
				.setOriginOrderId(UUID.randomUUID().toString())
				.setContract(contract)
				.setPrice(price)
				.setStopPrice(StringUtils.isNotBlank(orderReq.getStopPrice()) ? Double.parseDouble(orderReq.getStopPrice()) : 0D)
				.setOrderPriceType(price <= 0 ? OrderPriceTypeEnum.OPT_AnyPrice : OrderPriceTypeEnum.OPT_LimitPrice)
				.setDirection(orderDir)
				.setVolume(orderReq.getVolume())
				.setOffsetFlag(OffsetFlagEnum.OF_Open)
				.setHedgeFlag(HedgeFlagEnum.HF_Speculation)
				.setTimeCondition(TimeConditionEnum.TC_GFD)
				.setVolumeCondition(VolumeConditionEnum.VC_AV)
				.setForceCloseReason(ForceCloseReasonEnum.FCR_NotForceClose)
				.setContingentCondition(ContingentConditionEnum.CC_Immediately)
				.setMinVolume(1)
				.setGatewayId(orderReq.getGatewayId())
				.build();
	}
	
	private List<SubmitOrderReqField> generateCloseOrder(OrderRequest orderReq) {
		ContractField contract = contractMgr.getContract(Identifier.of(orderReq.getContractId())).contractField();
		if(contract == null) {
			throw new NoSuchElementException("不存在此合约：" + orderReq.getContractId());
		}
		DirectionEnum orderDir = OrderUtils.resolveDirection(orderReq.getTradeOpr());
		PositionDirectionEnum targetPosDir = OrderUtils.getClosingDirection(orderDir);
		TradeAccount account = accountMgr.get(Identifier.of(orderReq.getGatewayId()));
		PositionField pos = account.getPosition(targetPosDir, contract.getUnifiedSymbol())
				.orElseThrow(() -> new NoSuchElementException(String.format("不存在 [%s] 合约 [%s] 持仓", contract.getUnifiedSymbol(), targetPosDir)));
		int totalAvailable = pos.getPosition() - pos.getFrozen();
		int tdAvailable = pos.getTdPosition() - pos.getTdFrozen();
		int ydAvailable = pos.getYdPosition() - pos.getYdFrozen();
		if(totalAvailable < orderReq.getVolume()) {
			throw new InsufficientException("持仓不足，无法平仓");
		}
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
		
		int consume = 0;
		
		while(consume < orderReq.getVolume()) {
			if(ydAvailable > 0) {
				consume = Math.min(ydAvailable, orderReq.getVolume() - consume);
				ydAvailable -= consume;
				result.add(sb.setVolume(consume).setOffsetFlag(OffsetFlagEnum.OF_CloseYesterday).build());
			} else if(tdAvailable > 0) {
				consume = Math.min(tdAvailable, orderReq.getVolume() - consume);
				tdAvailable -= consume;
				result.add(sb.setVolume(consume).setOffsetFlag(OffsetFlagEnum.OF_CloseToday).build());
			} else {
				throw new TradeException();
			}
		}
		return result;
	}
	
	/**
	 * 撤单
	 * @return
	 * @throws TradeException 
	 */
	public boolean cancelOrder(OrderRecall withdrawReq) throws TradeException {
		log.info("撤单委托");
		String gatewayId = withdrawReq.getGatewayId();
		TradeAccount account = accountMgr.get(Identifier.of(gatewayId));
		if(account == null) {
			throw new NoSuchElementException("没有找到该账户实例：" + gatewayId);
		}
		CancelOrderReqField order = CancelOrderReqField.newBuilder()
				.setOriginOrderId(withdrawReq.getOriginOrderId())
				.setGatewayId(withdrawReq.getGatewayId())
				.build();
		account.cancelOrder(order);
		return true;
	}
	
}
