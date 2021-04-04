package tech.xuanwu.northstar.domain;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.StringUtils;

import com.google.common.eventbus.EventBus;

import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.common.exception.InsufficientException;
import tech.xuanwu.northstar.common.exception.TradeException;
import tech.xuanwu.northstar.common.model.OrderRecall;
import tech.xuanwu.northstar.common.model.OrderRequest;
import tech.xuanwu.northstar.common.utils.OrderUtil;
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
	private Map<String, ContractField> contractMap;
	
	private String accountId;
	
	private EventBus eventBus;
	
	public TradeDayAccount(String gatewayId, EventBus eventBus, Map<String, ContractField> contractMap) {
		this.accountId = gatewayId;
		this.eventBus = eventBus;
		this.contractMap = contractMap;
		this.posDescription = new PositionDescription(contractMap);
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
		ContractField contract = contractMap.get(orderReq.getContractSymbol());
		if(contract == null) {
			throw new NoSuchElementException("不存在此合约：" + orderReq.getContractSymbol());
		}
		// 检查可用资金
		double marginRate = OrderUtil.resolveDirection(orderReq.getTradeOpr()) == DirectionEnum.D_Buy ? contract.getLongMarginRatio() : contract.getShortMarginRatio();
		double totalCost = orderReq.getVolume() * Double.parseDouble(orderReq.getPrice()) * marginRate;
		if(accountInfo.getAvailable() < totalCost) {
			throw new InsufficientException("可用资金不足，无法开仓");
		}
		DirectionEnum orderDir = OrderUtil.resolveDirection(orderReq.getTradeOpr());
		SubmitOrderReqField req = SubmitOrderReqField.newBuilder()
				.setContract(contract)
				.setPrice(Double.parseDouble(orderReq.getPrice()))
				.setStopPrice(StringUtils.isNotBlank(orderReq.getStopPrice()) ? Double.parseDouble(orderReq.getStopPrice()) : 0D)
				.setOrderPriceType(OrderPriceTypeEnum.OPT_LimitPrice)
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
		eventBus.post(new NorthstarEvent(NorthstarEventType.PLACE_ORDER, req));
		return true;
	}
	
	public boolean closePosition(OrderRequest orderReq) throws InsufficientException {
		List<SubmitOrderReqField> orders = posDescription.generateCloseOrderReq(orderReq);
		orders.forEach(order -> eventBus.post(new NorthstarEvent(NorthstarEventType.PLACE_ORDER, order)));
		return true;
	}
	
	public boolean cancelOrder(OrderRecall orderRecall) throws TradeException {
		if(!tdOrder.canCancelOrder(orderRecall.getOrderId())) {
			throw new TradeException("没有可撤订单");
		}
		CancelOrderReqField order = CancelOrderReqField.newBuilder()
				.setOrderId(orderRecall.getOrderId())
				.build();
		eventBus.post(new NorthstarEvent(NorthstarEventType.WITHDRAW_ORDER, order));
		return true;
	}

	public TradeDayTransaction getTdTransaction() {
		return tdTransaction;
	}

	public void setTdTransaction(TradeDayTransaction tdTransaction) {
		this.tdTransaction = tdTransaction;
	}

	public TradeDayOrder getTdOrder() {
		return tdOrder;
	}

	public void setTdOrder(TradeDayOrder tdOrder) {
		this.tdOrder = tdOrder;
	}

	public PositionDescription getPosDescription() {
		return posDescription;
	}

	public void setPosDescription(PositionDescription posDescription) {
		this.posDescription = posDescription;
	}

	public AccountField getAccountInfo() {
		return accountInfo;
	}

	public void setAccountInfo(AccountField accountInfo) {
		this.accountInfo = accountInfo;
	}

	public String getAccountId() {
		return accountId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}
	
}
