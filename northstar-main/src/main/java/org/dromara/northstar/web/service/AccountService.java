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
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Position;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.utils.OrderUtils;
import org.dromara.northstar.gateway.IContractManager;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;
import xyz.redtorch.pb.CoreEnum.VolumeConditionEnum;

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
	
	private SubmitOrderReq generateOpenOrder(OrderRequest orderReq) {
		Contract contract = contractMgr.getContract(Identifier.of(orderReq.getContractId())).contract();
		if(contract == null) {
			throw new NoSuchElementException("不存在此合约：" + orderReq.getContractId());
		}
		// 检查可用资金
		double marginRate = OrderUtils.resolveDirection(orderReq.getTradeOpr()) == DirectionEnum.D_Buy ? contract.longMarginRatio() : contract.shortMarginRatio();
		double totalCost = orderReq.getVolume() * Double.parseDouble(orderReq.getPrice()) * contract.multiplier() * marginRate;
		TradeAccount account = accountMgr.get(Identifier.of(orderReq.getGatewayId()));
		if(account.availableAmount() < totalCost) {
			throw new InsufficientException("可用资金不足，无法开仓");
		}
		DirectionEnum orderDir = OrderUtils.resolveDirection(orderReq.getTradeOpr());
		OrderPriceTypeEnum priceType = switch(orderReq.getPriceType()) {
		case ANY_PRICE -> OrderPriceTypeEnum.OPT_AnyPrice;
		default -> OrderPriceTypeEnum.OPT_LimitPrice;
		};
		double price = Double.parseDouble(orderReq.getPrice());
		return SubmitOrderReq.builder()
				.originOrderId(UUID.randomUUID().toString())
				.contract(contract)
				.currency(contract.currency())
				.price(price)
				.stopPrice(StringUtils.isNotBlank(orderReq.getStopPrice()) ? Double.parseDouble(orderReq.getStopPrice()) : 0D)
				.orderPriceType(priceType)
				.direction(orderDir)
				.volume(orderReq.getVolume())
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.timeCondition(TimeConditionEnum.TC_GFD)
				.contingentCondition(ContingentConditionEnum.CC_Immediately)
				.volumeCondition(VolumeConditionEnum.VC_AV)
				.minVolume(1)
				.gatewayId(orderReq.getGatewayId())
				.build();
	}
	
	private List<SubmitOrderReq> generateCloseOrder(OrderRequest orderReq) {
		Contract contract = contractMgr.getContract(Identifier.of(orderReq.getContractId())).contract();
		if(contract == null) {
			throw new NoSuchElementException("不存在此合约：" + orderReq.getContractId());
		}
		DirectionEnum orderDir = OrderUtils.resolveDirection(orderReq.getTradeOpr());
		PositionDirectionEnum targetPosDir = OrderUtils.getClosingDirection(orderDir);
		TradeAccount account = accountMgr.get(Identifier.of(orderReq.getGatewayId()));
		Position pos = account.getPosition(targetPosDir, contract)
				.orElseThrow(() -> new NoSuchElementException(String.format("不存在 [%s] 合约 [%s] 持仓", contract.unifiedSymbol(), targetPosDir)));
		int totalAvailable = pos.position() - pos.frozen();
		int tdAvailable = pos.tdPosition() - pos.tdFrozen();
		int ydAvailable = pos.ydPosition() - pos.ydFrozen();
		if(totalAvailable < orderReq.getVolume()) {
			throw new InsufficientException("持仓不足，无法平仓");
		}
		List<SubmitOrderReq> result = new ArrayList<>();
		OrderPriceTypeEnum priceType = switch(orderReq.getPriceType()) {
		case ANY_PRICE -> OrderPriceTypeEnum.OPT_AnyPrice;
		default -> OrderPriceTypeEnum.OPT_LimitPrice;
		};
		double price = Double.parseDouble(orderReq.getPrice());
		SubmitOrderReq proto = SubmitOrderReq.builder()
				.originOrderId(UUID.randomUUID().toString())
				.contract(contract)
				.currency(contract.currency())
				.price(price)
				.stopPrice(StringUtils.isNotBlank(orderReq.getStopPrice()) ? Double.parseDouble(orderReq.getStopPrice()) : 0D)
				.orderPriceType(priceType)
				.direction(orderDir)
				.timeCondition(TimeConditionEnum.TC_GFD)
				.contingentCondition(ContingentConditionEnum.CC_Immediately)
				.volumeCondition(VolumeConditionEnum.VC_AV)
				.minVolume(1)
				.gatewayId(orderReq.getGatewayId())
				.build();

		int totalConsume = 0;
		
		while(totalConsume < orderReq.getVolume()) {
			if(ydAvailable > 0) {
				int consume = Math.min(ydAvailable, orderReq.getVolume() - totalConsume);
				ydAvailable -= consume;
				result.add(proto.toBuilder().volume(consume).offsetFlag(OffsetFlagEnum.OF_CloseYesterday).build());
				totalConsume += consume;
			} else if(tdAvailable > 0) {
				int consume = Math.min(tdAvailable, orderReq.getVolume() - totalConsume);
				tdAvailable -= consume;
				result.add(proto.toBuilder().volume(consume).offsetFlag(OffsetFlagEnum.OF_CloseToday).build());
				totalConsume += consume;
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
		account.cancelOrder(withdrawReq.getOriginOrderId());
		return true;
	}
	
}
