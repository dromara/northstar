package tech.quantit.northstar.gateway.sim.trade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import com.google.common.eventbus.EventBus;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tech.quantit.northstar.common.TickDataAware;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.utils.FieldUtils;
import xyz.redtorch.pb.CoreEnum.CurrencyEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

@Data
@Document
@NoArgsConstructor
public class SimAccount implements TickDataAware{
	
	@Id
	private String gatewayId;

	// 委托请求不需要持久化
	@Transient
	protected Set<OpenTradeRequest> openReqSet = new HashSet<>();
	@Transient
	protected Set<CloseTradeRequest> closeReqSet = new HashSet<>();

	protected Map<String, SimPosition> longMap = new HashMap<>();
	protected Map<String, SimPosition> shortMap = new HashMap<>();
	
	protected double totalCloseProfit;
	
	protected double totalCommission;
	
	protected double totalDeposit;
	
	protected double totalWithdraw;
	
	protected int transactionFee;
	
	@Transient
	@Setter
	protected Runnable savingCallback;
	@Transient
	private EventBus eventBus;
	@Transient
	@Setter
	private FastEventEngine feEngine;
	@Transient
	protected Consumer<TradeRequest> openCallback = req -> {
		if(req.isDone()) {			
			eventBus.unregister(req);
			CompletableFuture.runAsync(() -> openReqSet.remove(req), CompletableFuture.delayedExecutor(300, TimeUnit.MILLISECONDS));
		}
		savingCallback.run();
	};
	@Transient
	Consumer<TradeRequest> closeCallback = req -> {
		if(req.isDone()) {			
			eventBus.unregister(req);
			CompletableFuture.runAsync(() -> closeReqSet.remove(req), CompletableFuture.delayedExecutor(300, TimeUnit.MILLISECONDS));
		}
		savingCallback.run();
	};
	
	public SimAccount(String gatewayId,  int fee) {
		this.gatewayId = gatewayId;
		this.transactionFee = fee;
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
		return openReqSet.stream().mapToDouble(OpenTradeRequest::frozenAmount).reduce((a, b) -> a + b).orElse(0);
	}
	
	public double totalMargin() {
		double longPositionFrozen = longMap.values().stream().mapToDouble(SimPosition::frozenMargin).reduce((a, b) -> a + b).orElse(0);
		double shortPositionFrozen = shortMap.values().stream().mapToDouble(SimPosition::frozenMargin).reduce((a, b) -> a + b).orElse(0);
		return longPositionFrozen + shortPositionFrozen;
	}
	
	public double positionProfit() {
		double longPositionProfit = longMap.values().stream().mapToDouble(SimPosition::profit).reduce((a, b) -> a + b).orElse(0);
		double shortPositionProfit = shortMap.values().stream().mapToDouble(SimPosition::profit).reduce((a, b) -> a + b).orElse(0);
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
		list.addAll(longMap.values().stream().map(SimPosition::positionField).toList());
		list.addAll(shortMap.values().stream().map(SimPosition::positionField).toList());
		return list;
	}
	
	public void addPosition(SimPosition position, TradeField trade) {
		Map<String, SimPosition> posMap = FieldUtils.isLong(position.getDirection()) ? longMap : shortMap;
		if(posMap.containsKey(position.getUnifiedSymbol())) {
			SimPosition originPosition = posMap.get(position.getUnifiedSymbol());
			originPosition.merge(trade);
		} else {			
			posMap.put(position.getUnifiedSymbol(), position);
			eventBus.register(position);
		}
	}
	
	public void onSubmitOrder(SubmitOrderReqField orderReq) {
		if(FieldUtils.isOpen(orderReq.getOffsetFlag())) {
			OpenTradeRequest tradeReq = new OpenTradeRequest(this, feEngine, orderReq, openCallback);
			if(!tradeReq.isValid()) {
				return;
			}
			eventBus.register(tradeReq);
			openReqSet.add(tradeReq);
		}
		
		if(FieldUtils.isClose(orderReq.getOffsetFlag())) {
			SimPosition position = getClosingPositionByReq(orderReq);
			CloseTradeRequest tradeReq = new CloseTradeRequest(this, position, feEngine, orderReq, closeCallback);
			if(!tradeReq.isValid()) {
				return;
			}
			position.setCloseReq(tradeReq);
			eventBus.register(tradeReq);
			closeReqSet.add(tradeReq);
		}
	}
	
	private Map<String, SimPosition> getClosingMap(DirectionEnum direction){
		if(FieldUtils.isBuy(direction)) 
			return shortMap;
		if(FieldUtils.isSell(direction))
			return longMap;
		throw new IllegalStateException("平仓方向不明确");
	}
	
	private SimPosition getClosingPositionByReq(SubmitOrderReqField orderReq) {
		SimPosition pos = getClosingMap(orderReq.getDirection()).get(orderReq.getContract().getUnifiedSymbol());
		if(pos == null)
			throw new IllegalStateException("没有找到可以平仓的合约");
		return pos;
	}
	
	public void onCancelOrder(CancelOrderReqField cancelReq) {
		eventBus.post(cancelReq);
	}
	
	public void setEventBus(EventBus eventBus) {
		this.eventBus = eventBus;
		eventBus.register(this);
		longMap.values().stream().forEach(eventBus::register);
		shortMap.values().stream().forEach(eventBus::register);
	}

	public void addCommission(int volume) {
		totalCommission += volume * transactionFee;
	}

	public void addCloseProfit(double profit) {
		totalCloseProfit += profit;
	}

	private long lastReportTime;
	@Override
	public void onTick(TickField tick) {
		if(System.currentTimeMillis() - lastReportTime < 1000) {
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
	
	private void reportPosition(Map<String, SimPosition> posMap) {
		Iterator<Entry<String, SimPosition>> itEntry = posMap.entrySet().iterator();
		while(itEntry.hasNext()) {
			Entry<String, SimPosition> e = itEntry.next();
			PositionField pf = e.getValue().positionField();
			feEngine.emitEvent(NorthstarEventType.POSITION, pf);
			if(pf.getPosition() == 0) {
				itEntry.remove();
			}
		}
	}
}
