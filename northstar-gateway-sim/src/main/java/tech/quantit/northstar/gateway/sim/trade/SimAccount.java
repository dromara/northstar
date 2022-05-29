package tech.quantit.northstar.gateway.sim.trade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import com.google.common.eventbus.EventBus;
import com.google.protobuf.InvalidProtocolBufferException;

import lombok.Data;
import lombok.Setter;
import tech.quantit.northstar.common.IContractManager;
import tech.quantit.northstar.common.TickDataAware;
import tech.quantit.northstar.common.constant.ClosingPolicy;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.model.ContractDefinition;
import tech.quantit.northstar.common.model.SimAccountDescription;
import tech.quantit.northstar.common.utils.FieldUtils;
import tech.quantit.northstar.common.utils.MessagePrinter;
import xyz.redtorch.pb.CoreEnum.CurrencyEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

@Data
public class SimAccount implements TickDataAware{
	
	private String gatewayId;

	// originOrderId -> tradeRequest
	protected Map<String, OpenTradeRequest> openReqMap = new HashMap<>();
	protected Map<String, CloseTradeRequest> closeReqMap = new HashMap<>();

	protected Map<ContractField, TradePosition> longMap = new HashMap<>();
	protected Map<ContractField, TradePosition> shortMap = new HashMap<>();
	
	protected double totalCloseProfit;
	
	protected double totalCommission;
	
	protected double totalDeposit;
	
	protected double totalWithdraw;
	
	private volatile boolean connected;
	
	private IContractManager contractMgr;
	
	@Setter
	protected Runnable savingCallback;
	private EventBus eventBus;
	@Setter
	private FastEventEngine feEngine;
	protected Consumer<TradeRequest> openCallback = req -> {
		eventBus.unregister(req);
		savingCallback.run();
		openReqMap.remove(req.originOrderId());
	};
	Consumer<TradeRequest> closeCallback = req -> {
		eventBus.unregister(req);
		savingCallback.run();
		closeReqMap.remove(req.originOrderId());
	};
	
	public SimAccount(String gatewayId,  IContractManager contractMgr) {
		this.gatewayId = gatewayId;
		this.contractMgr = contractMgr;
	}
	
	public SimAccount(SimAccountDescription simAccDescription, IContractManager contractMgr) throws InvalidProtocolBufferException {
		this.gatewayId = simAccDescription.getGatewayId();
		this.contractMgr = contractMgr;
		this.totalCloseProfit = simAccDescription.getTotalCloseProfit();
		this.totalCommission = simAccDescription.getTotalCommission();
		this.totalDeposit = simAccDescription.getTotalDeposit();
		this.totalWithdraw = simAccDescription.getTotalWithdraw();
		for(byte[] uncloseTradeData : simAccDescription.getOpenTrades()) {
			TradeField uncloseTrade = TradeField.parseFrom(uncloseTradeData);
			Map<ContractField, TradePosition> tMap = getOpeningMap(uncloseTrade.getDirection());
			if(tMap.containsKey(uncloseTrade.getContract())) {
				tMap.get(uncloseTrade.getContract()).onTrade(uncloseTrade);
			}else {
				tMap.put(uncloseTrade.getContract(), new TradePosition(List.of(uncloseTrade), ClosingPolicy.FIFO));
			}
		}
	}
	
	public double balance() {
		// 当前权益 = 平仓盈亏 + 持仓盈亏 - 手续费 + 入金金额 - 出金金额
		return totalCloseProfit + positionProfit() - totalCommission + totalDeposit - totalWithdraw;
	}
	
	public double available() {
		// 可用资金 = 当前权益 - 持仓保证金 - 委托单保证金
		return balance() - totalMargin() - totalFrozen();
	}
	
	public double totalFrozen() {
		return openReqMap.values().stream().mapToDouble(OpenTradeRequest::frozenAmount).reduce((a, b) -> a + b).orElse(0);
	}
	
	public double totalMargin() {
		double longPositionFrozen = longMap.values().stream().mapToDouble(tp -> tp.totalVolume() - tp.totalAvailable()).reduce((a, b) -> a + b).orElse(0);
		double shortPositionFrozen = shortMap.values().stream().mapToDouble(tp -> tp.totalVolume() - tp.totalAvailable()).reduce((a, b) -> a + b).orElse(0);
		return longPositionFrozen + shortPositionFrozen;
	}
	
	public double positionProfit() {
		double longPositionProfit = longMap.values().stream().mapToDouble(TradePosition::profit).reduce((a, b) -> a + b).orElse(0);
		double shortPositionProfit = shortMap.values().stream().mapToDouble(TradePosition::profit).reduce((a, b) -> a + b).orElse(0);
		return longPositionProfit + shortPositionProfit;
	}
	
	public void depositMoney(int money) {
		if(money < 0) {
			throw new IllegalArgumentException("金额不少于0");
		}
		totalDeposit += money;
		savingCallback.run();
		feEngine.emitEvent(NorthstarEventType.ACCOUNT, accountField());
	}
	
	public void withdrawMoney(int money) {
		if(money < 0) {
			throw new IllegalArgumentException("金额不少于0");
		}
		if(available() < money) {
			throw new IllegalStateException("余额不足");
		}
		totalWithdraw += money;
		savingCallback.run();
		feEngine.emitEvent(NorthstarEventType.ACCOUNT, accountField());
	}

	public AccountField accountField() {
		return AccountField.newBuilder()
				.setAccountId(getGatewayId())
				.setAvailable(available())
				.setBalance(balance())
				.setCloseProfit(totalCloseProfit)
				.setCommission(totalCommission)
				.setGatewayId(getGatewayId())
				.setCurrency(CurrencyEnum.CNY)
				.setName("模拟账户")
				.setDeposit(totalDeposit)
				.setWithdraw(totalWithdraw)
				.setMargin(totalMargin())
				.setPositionProfit(positionProfit())
				.build();
	}
	
	public List<PositionField> positionFields(){
		int size = longMap.size() + shortMap.size();
		if(size == 0)
			return Collections.emptyList();
		List<PositionField> list = new ArrayList<>(size);
		list.addAll(longMap.values().stream().map(tp -> tp.convertToPositionField(this)).toList());
		list.addAll(shortMap.values().stream().map(tp -> tp.convertToPositionField(this)).toList());
		return list;
	}
	
	public void onSubmitOrder(SubmitOrderReqField orderReq) {
		if(FieldUtils.isOpen(orderReq.getOffsetFlag())) {
			OpenTradeRequest tradeReq = new OpenTradeRequest(this, feEngine, openCallback);
			OrderField order = tradeReq.initOrder(orderReq);
			if(order.getOrderStatus() == OrderStatusEnum.OS_Rejected) {
				return;
			}
			eventBus.register(tradeReq);
			openReqMap.put(tradeReq.originOrderId(), tradeReq);
		}
		
		if(FieldUtils.isClose(orderReq.getOffsetFlag())) {
			TradePosition position = getClosingPositionByReq(orderReq);
			CloseTradeRequest tradeReq = new CloseTradeRequest(this, position, feEngine, closeCallback);
			OrderField order = tradeReq.initOrder(orderReq);
			if(order.getOrderStatus() == OrderStatusEnum.OS_Rejected) {
				return;
			}
			position.onOrder(order);
			eventBus.register(tradeReq);
			closeReqMap.put(tradeReq.originOrderId(), tradeReq);
		}
	}
	
	private Map<ContractField, TradePosition> getOpeningMap(DirectionEnum direction){
		return switch(direction) {
		case D_Buy -> longMap;
		case D_Sell -> shortMap;
		default -> throw new IllegalStateException("开仓方向不明确");
		};
	}
	
	private Map<ContractField, TradePosition> getClosingMap(DirectionEnum direction){
		return switch(direction) {
		case D_Buy -> shortMap;
		case D_Sell -> longMap;
		default -> throw new IllegalStateException("平仓方向不明确");
		};
	}
	
	private TradePosition getClosingPositionByReq(SubmitOrderReqField orderReq) {
		TradePosition pos = getClosingMap(orderReq.getDirection()).get(orderReq.getContract());
		if(pos == null)
			throw new IllegalStateException("没有找到可以平仓的合约");
		return pos;
	}
	
	public void onCancelOrder(CancelOrderReqField cancelReq) {
		eventBus.post(cancelReq);
	}
	
	public void setEventBus(EventBus eventBus) {
		this.eventBus = eventBus;
		longMap.values().stream().forEach(eventBus::register);
		shortMap.values().stream().forEach(eventBus::register);
	}
	
	public void onOpenTrade(TradeField trade) {
		Map<ContractField, TradePosition> tMap = getOpeningMap(trade.getDirection());
		if(tMap.containsKey(trade.getContract())) {
			tMap.get(trade.getContract()).onTrade(trade);
		} else {
			tMap.put(trade.getContract(), new TradePosition(List.of(trade), ClosingPolicy.FIFO));
		}
		onTrade(trade);
	}
	
	public void onCloseTrade(TradeField trade) {
		Map<ContractField, TradePosition> tMap = getClosingMap(trade.getDirection());
		if(!tMap.containsKey(trade.getContract())) {
			throw new IllegalStateException("没有对应持仓可以对冲当前成交：" + MessagePrinter.print(trade));
		}
		tMap.get(trade.getContract()).onTrade(trade);
		onTrade(trade);
	}

	private void onTrade(TradeField trade) {
		ContractDefinition contractDef = contractMgr.getContractDefinition(trade.getContract().getUnifiedSymbol());
		double commission = contractDef.getCommissionInPrice() > 0 ? contractDef.getCommissionInPrice() : contractDef.commissionRate() * trade.getPrice() * trade.getContract().getMultiplier();
		totalCommission += trade.getVolume() * commission;
	}

	public void addCloseProfit(double profit) {
		totalCloseProfit += profit;
	}

	private long lastReportTime;
	@Override
	public void onTick(TickField tick) {
		longMap.values().stream().forEach(tp -> tp.updateTick(tick));
		shortMap.values().stream().forEach(tp -> tp.updateTick(tick));
		if(!connected || System.currentTimeMillis() - lastReportTime < 1000) {
			return;
		}
		lastReportTime = System.currentTimeMillis();
		reportAccountStatus();
	}
	
	protected void reportAccountStatus() {
		feEngine.emitEvent(NorthstarEventType.ACCOUNT, accountField());
		reportPosition(longMap);
		reportPosition(shortMap);
	}
	
	private void reportPosition(Map<ContractField, TradePosition> posMap) {
		Iterator<Entry<ContractField, TradePosition>> itEntry = posMap.entrySet().iterator();
		while(itEntry.hasNext()) {
			Entry<ContractField, TradePosition> e = itEntry.next();
			PositionField pf = e.getValue().convertToPositionField(this);
			feEngine.emitEvent(NorthstarEventType.POSITION, pf);
			if(pf.getPosition() == 0) {
				itEntry.remove();
			}
		}
	}
	
	public void setConnected(boolean connected) {
		this.connected = connected;
	}

	public SimAccountDescription getDescription() {
		List<TradeField> uncloseTrades = new ArrayList<>();
		longMap.values().stream().forEach(tp -> uncloseTrades.addAll(tp.getUncloseTrades()));
		shortMap.values().stream().forEach(tp -> uncloseTrades.addAll(tp.getUncloseTrades()));
		return SimAccountDescription.builder()
				.gatewayId(gatewayId)
				.totalCloseProfit(totalCloseProfit)
				.totalCloseProfit(totalCloseProfit)
				.totalDeposit(totalDeposit)
				.totalWithdraw(totalWithdraw)
				.openTrades(uncloseTrades.stream().map(TradeField::toByteArray).toList())
				.build();
	}
}
