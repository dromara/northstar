package tech.quantit.northstar.gateway.sim.trade;

import java.util.stream.Collectors;

import com.google.common.util.concurrent.AtomicDouble;
import com.google.protobuf.InvalidProtocolBufferException;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.exception.TradeException;
import tech.quantit.northstar.gateway.sim.persistence.SimAccountPO;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

@Slf4j
class GwAccountHolder {

	private AccountField.Builder accBuilder;

	private FastEventEngine feEngine;

	private GwOrderHolder orderHolder;

	private GwPositionHolder posHolder;

	private int fee;
	
	private TickField lastTick;
	
	private long lastEmitTime;
	
	//测试用的后门开关
	public boolean testFlag;
	
	public GwAccountHolder(String gatewayId, FastEventEngine feEngine, int fee, SimFactory factory) {
		this.feEngine = feEngine;
		this.accBuilder = AccountField.newBuilder()
				.setName(gatewayId + "模拟账户")
				.setAccountId(gatewayId)
				.setGatewayId(gatewayId);
		this.orderHolder = factory.newGwOrderHolder();
		this.posHolder = factory.newGwPositionHolder();
		this.fee = fee;
	}

	protected void updateTick(TickField tick) {
		lastTick = tick;
		AtomicDouble commission = new AtomicDouble();
		AtomicDouble closeProfit = new AtomicDouble();
		orderHolder.tryDeal(tick)
			.stream()
			.forEach(order -> {
				TradeField trade = orderHolder.transform(order);
				if(order.getOffsetFlag() == OffsetFlagEnum.OF_Open) {
					//手续费只按单边计算
					commission.addAndGet(fee * trade.getVolume());
				}
				closeProfit.addAndGet(posHolder.updatePositionBy(trade));
				posHolder.updatePositionBy(order);
				feEngine.emitEvent(NorthstarEventType.ORDER, order);
				feEngine.emitEvent(NorthstarEventType.TRADE, trade);
				try {
					// 阻塞一下，好让模组收到成交事件
					Thread.sleep(50);
				} catch (InterruptedException e) {
					log.warn("", e);
				}
			});
		posHolder.updatePositionBy(tick)
			.stream()
			.filter(opt -> opt.isPresent())
			.forEach(opt -> feEngine.emitEvent(NorthstarEventType.POSITION, opt.get()));
		double frozenMargin = orderHolder.getFrozenMargin();
		double useMargin = posHolder.getTotalUseMargin();
		double positionProfit = posHolder.getTotalPositionProfit();
		accBuilder.setCloseProfit(accBuilder.getCloseProfit() + closeProfit.get());
		accBuilder.setCommission(accBuilder.getCommission() + commission.get());
		accBuilder.setPositionProfit(positionProfit);
		accBuilder.setMargin(frozenMargin + useMargin);
		refreshAccount();
	}

	private void refreshAccount() {
		// 当前权益 = 期初权益 + 当天平仓盈亏 + 持仓盈亏 - 手续费 + 入金金额 - 出金金额
		accBuilder.setBalance(accBuilder.getPreBalance() + accBuilder.getCloseProfit() + accBuilder.getPositionProfit()
				- accBuilder.getCommission() + accBuilder.getDeposit() - accBuilder.getWithdraw());

		// 可用资金 = 当前权益 - 持仓保证金 - 委托单保证金
		accBuilder.setAvailable(accBuilder.getBalance() - accBuilder.getMargin());

		if(testFlag || System.currentTimeMillis() - lastEmitTime > 1000) {			
			lastEmitTime = System.currentTimeMillis();
			feEngine.emitEvent(NorthstarEventType.ACCOUNT, accBuilder.build());
		}
	}

	/**
	 * 入金
	 * 
	 * @param money
	 */
	protected int deposit(int money) {
		if (money < 0) {
			throw new IllegalArgumentException("入金金额不能少于零");
		}
		accBuilder.setDeposit(accBuilder.getDeposit() + money);
		refreshAccount();
		//需要立即更新账户
		feEngine.emitEvent(NorthstarEventType.ACCOUNT, accBuilder.build());
		return (int) accBuilder.getBalance();
	}

	/**
	 * 出金
	 * 
	 * @param money
	 */
	protected int withdraw(int money) {
		if (money < 0) {
			throw new IllegalArgumentException("出金金额不能少于零");
		}
		if(money > accBuilder.getAvailable()) {
			throw new IllegalStateException("没有足够余额出金");
		}
		accBuilder.setWithdraw(accBuilder.getWithdraw() + money);
		refreshAccount();
		//需要立即更新账户
		feEngine.emitEvent(NorthstarEventType.ACCOUNT, accBuilder.build());
		return (int) accBuilder.getBalance();
	}

	/**
	 * 委托下单
	 * @param submitOrderReq
	 * @return
	 */
	protected String submitOrder(SubmitOrderReqField submitOrderReq) {
		if(submitOrderReq.getOffsetFlag() == OffsetFlagEnum.OF_Unknown) {
			throw new TradeException("未定义委托操作");
		}
		OrderField order;
		if(submitOrderReq.getOffsetFlag() == OffsetFlagEnum.OF_Open) {
			order = orderHolder.tryOrder(submitOrderReq, accBuilder.build(), lastTick);
		} else {
			PositionField pf = posHolder.getPositionByReq(submitOrderReq);
			order = orderHolder.tryOrder(submitOrderReq, pf);
		}
		PositionField pf = posHolder.updatePositionBy(order);
		if(pf != null) {			
			feEngine.emitEvent(NorthstarEventType.POSITION, pf);
		}
		feEngine.emitEvent(NorthstarEventType.ORDER, order);
		refreshAccount();
		return order.getOrderId();
	}

	/**
	 * 委托撤单
	 * @param cancelOrderReq
	 * @return
	 */
	protected boolean cancelOrder(CancelOrderReqField cancelOrderReq) {
		OrderField order = orderHolder.cancelOrder(cancelOrderReq);
		if(order == null) {
			return false;
		}
		PositionField pf = posHolder.updatePositionBy(order);
		if(pf != null){			
			feEngine.emitEvent(NorthstarEventType.POSITION, pf);
		}
		feEngine.emitEvent(NorthstarEventType.ORDER, order);
		refreshAccount();
		return true;
	}
	
	protected SimAccountPO convertTo() {
		SimAccountPO po = new SimAccountPO();
		po.setGatewayId(accBuilder.getGatewayId());
		po.setAccountData(accBuilder.build().toByteArray());
		po.setPositionData(posHolder.getPositions()
				.stream()
				.map(pf -> pf.toByteArray())
				.collect(Collectors.toList()));
		return po;
	}
	
	protected void convertFrom(SimAccountPO po) throws InvalidProtocolBufferException {
		accBuilder = AccountField.newBuilder().mergeFrom(po.getAccountData());
		posHolder.restore(po.getPositionData());
	}
	
	protected void emitStatus() {
		feEngine.emitEvent(NorthstarEventType.ACCOUNT, accBuilder.build());
		for(PositionField pf : posHolder.getPositions()) {					
			feEngine.emitEvent(NorthstarEventType.POSITION, pf);
		}
	}

	/**
	 * 日结算
	 */
	protected void dailySettlement() {
		// FIXME Not done
	}

}
