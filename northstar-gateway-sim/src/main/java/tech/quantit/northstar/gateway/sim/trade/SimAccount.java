package tech.quantit.northstar.gateway.sim.trade;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.utils.FieldUtils;
import xyz.redtorch.pb.CoreEnum.CurrencyEnum;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TradeField;

@Data
@Document
@NoArgsConstructor
public class SimAccount {
	
	@Id
	private String gatewayId;

	// 委托请求不需要持久化
	@Transient
	protected Set<OpenTradeRequest> openReqSet = Sets.newConcurrentHashSet();
	@Transient
	protected Set<CloseTradeRequest> closeReqSet = Sets.newConcurrentHashSet();
	
	protected ConcurrentMap<String, SimPosition> longMap = new ConcurrentHashMap<>();
	protected ConcurrentMap<String, SimPosition> shortMap = new ConcurrentHashMap<>();
	
	protected double totalCloseProfit;
	
	protected double totalCommission;
	
	protected double totalDeposit;
	
	protected double totalWithdraw;
	
	protected int transactionFee;
	
	@Transient
	protected Runnable savingCallback;
	@Transient
	@Setter
	private EventBus eventBus;
	@Transient
	@Setter
	private FastEventEngine feEngine;
	
	protected Consumer<TradeRequest> openCallback = req -> {
		if(req.isDone()) {			
			eventBus.unregister(req);
			// 延迟十秒再移除，以免撤单异常
			CompletableFuture.runAsync(() -> openReqSet.remove(req), CompletableFuture.delayedExecutor(10, TimeUnit.SECONDS));
		}
		if(req.isTraded()) {
			TradeField trade = req.tradeField().get();
			ContractField contract = trade.getContract();
			if(FieldUtils.isBuy(trade.getDirection())) {
				longMap.put(contract.getUnifiedSymbol(), new SimPosition(trade));
			}
			if(FieldUtils.isSell(trade.getDirection())) {
				shortMap.put(contract.getUnifiedSymbol(), new SimPosition(trade));
			}
			if(savingCallback != null) {				
				savingCallback.run();
			}
		}
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
			Consumer<TradeRequest> closeCallback = req -> {
				if(req.isDone()) {			
					eventBus.unregister(req);
					// 延迟十秒再移除，以免撤单异常
					CompletableFuture.runAsync(() -> closeReqSet.remove(req), CompletableFuture.delayedExecutor(10, TimeUnit.SECONDS));
				}
				if(!req.isTraded()) {
					position.setCloseReq(null);
				} else if(savingCallback != null) {
					savingCallback.run();
				}
			};
			CloseTradeRequest tradeReq = new CloseTradeRequest(position, feEngine, orderReq, closeCallback);
			if(!tradeReq.isValid()) {
				return;
			}
			position.setCloseReq(tradeReq);
			eventBus.register(tradeReq);
			closeReqSet.add(tradeReq);
		}
	}
	
	private SimPosition getClosingPositionByReq(SubmitOrderReqField orderReq) {
		SimPosition pos = null;
		if(FieldUtils.isBuy(orderReq.getDirection())) 
			pos = shortMap.get(orderReq.getContract().getUnifiedSymbol());
		if(FieldUtils.isSell(orderReq.getDirection()))
			pos = longMap.get(orderReq.getContract().getUnifiedSymbol());
		if(pos == null)
			throw new IllegalStateException("没有找到可以平仓的合约");
		return pos;
	}
	
	public void onCancelOrder(CancelOrderReqField cancelReq) {
		eventBus.post(cancelReq);
	}
	
	public void setSavingCallback(Runnable savingCallback) {
		this.savingCallback = savingCallback;
		longMap.values().stream().forEach(pos -> pos.setSavingCallback(savingCallback));
		shortMap.values().stream().forEach(pos -> pos.setSavingCallback(savingCallback));
	}
}
