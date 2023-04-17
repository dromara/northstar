package org.dromara.northstar.account;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.exception.InsufficientException;
import org.dromara.northstar.common.exception.TradeException;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.OrderRecall;
import org.dromara.northstar.common.model.OrderRequest;
import org.dromara.northstar.common.utils.OrderUtils;
import org.dromara.northstar.gateway.api.IContractManager;
import org.dromara.northstar.gateway.api.TradeGateway;

import lombok.Getter;
import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.ForceCloseReasonEnum;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;
import xyz.redtorch.pb.CoreEnum.VolumeConditionEnum;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 交易日账户
 * @author KevinHuangwl
 *
 */
public class TradeDayAccount {
	
	protected TradeDayTransaction tdTransaction = new TradeDayTransaction();
	protected TradeDayOrder tdOrder = new TradeDayOrder();
	protected PositionDescription posDescription;
	
	private volatile AccountField accountInfo;
	private IContractManager contractMgr;
	
	private String accountId;
	@Getter
	protected TradeGateway gateway;
	
	public TradeDayAccount(String gatewayId, TradeGateway gateway, IContractManager contractMgr) {
		this.accountId = gatewayId;
		this.contractMgr = contractMgr;
		this.gateway = gateway;
		this.posDescription = new PositionDescription(contractMgr);
		this.accountInfo = AccountField.newBuilder()
				.setAccountId(gatewayId)
				.build();
	}
	
	public void onAccountUpdate(AccountField account) {
		this.accountInfo = account;
	}
	
	public void onPositionUpdate(PositionField position) {
		posDescription.update(position);
	}
	
	public void onTradeUpdate(TradeField trade) {
		tdTransaction.update(trade);
	}
	
	public void onOrderUpdate(OrderField order) {
		tdOrder.update(order);
	}
	
	public boolean openPosition(OrderRequest orderReq) throws InsufficientException {
		ContractField contract = contractMgr.getContract(Identifier.of(orderReq.getContractId())).contractField();
		if(contract == null) {
			throw new NoSuchElementException("不存在此合约：" + orderReq.getContractId());
		}
		// 检查可用资金
		double marginRate = OrderUtils.resolveDirection(orderReq.getTradeOpr()) == DirectionEnum.D_Buy ? contract.getLongMarginRatio() : contract.getShortMarginRatio();
		double totalCost = orderReq.getVolume() * Double.parseDouble(orderReq.getPrice()) * contract.getMultiplier() * marginRate;
		if(accountInfo.getAvailable() < totalCost) {
			throw new InsufficientException("可用资金不足，无法开仓");
		}
		DirectionEnum orderDir = OrderUtils.resolveDirection(orderReq.getTradeOpr());
		double price = Double.parseDouble(orderReq.getPrice());
		SubmitOrderReqField req = SubmitOrderReqField.newBuilder()
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
		gateway.submitOrder(req);
		return true;
	}
	
	public boolean closePosition(OrderRequest orderReq) throws InsufficientException {
		List<SubmitOrderReqField> orders = posDescription.generateCloseOrderReq(orderReq);
		orders.forEach(gateway::submitOrder);
		return true;
	}
	
	public boolean cancelOrder(OrderRecall orderRecall) throws TradeException {
		if(!tdOrder.canCancelOrder(orderRecall.getOriginOrderId())) {
			throw new TradeException("没有可撤订单");
		}
		CancelOrderReqField order = CancelOrderReqField.newBuilder()
				.setOriginOrderId(orderRecall.getOriginOrderId())
				.setGatewayId(orderRecall.getGatewayId())
				.build();
		gateway.cancelOrder(order);
		return true;
	}

	
	public AccountField getAccountInfo() {
		return accountInfo;
	}

	public String getAccountId() {
		return accountId;
	}

	public List<PositionField> getPositions(){
		return posDescription.getPositions();
	}
	
	public List<TradeField> getTradeDayTransactions(){
		return tdTransaction.getTransactions();
	}
	
	public List<OrderField> getTradeDayOrders(){
		return tdOrder.getOrders();
	}
}
