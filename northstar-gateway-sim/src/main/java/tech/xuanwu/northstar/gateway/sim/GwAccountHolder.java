package tech.xuanwu.northstar.gateway.sim;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.protobuf.InvalidProtocolBufferException;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.common.exception.TradeException;
import tech.xuanwu.northstar.engine.event.FastEventEngine;
import tech.xuanwu.northstar.gateway.sim.persistence.SimAccountPO;
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

	private int ticksOfCommission;
	
	private SimGateway simGateway;
	
	private volatile long lastAccountEventTime;
	
	private ScheduledExecutorService exeService = Executors.newScheduledThreadPool(1);
	
	public GwAccountHolder(String gatewayId, FastEventEngine feEngine, int ticksOfCommission, SimFactory factory,
			SimGateway simGateway) {
		this.feEngine = feEngine;
		this.accBuilder = AccountField.newBuilder()
				.setName(gatewayId + "模拟账户")
				.setAccountId(gatewayId)
				.setGatewayId(gatewayId);
		this.orderHolder = factory.newGwOrderHolder();
		this.posHolder = factory.newGwPositionHolder();
		this.ticksOfCommission = ticksOfCommission;
		this.simGateway = simGateway;
		exeService.scheduleWithFixedDelay(()->{
			if(System.currentTimeMillis() - lastAccountEventTime > 2000) {
				refreshAccount();
			}
		}, 30, 1, TimeUnit.SECONDS);
	}

	protected void updateTick(TickField tick) {
		List<TradeField> tfs = orderHolder.tryDeal(tick);
		double commission = tfs.stream().reduce(0D,
				(acc, tf) -> acc + tf.getContract().getPriceTick() * ticksOfCommission, 
				(acc, tf) -> 0D);
		double closeProfit = tfs.stream()
				.map(tf -> posHolder.updatePositionBy(tf))
				.reduce(0D, (a, b) -> a + b);
		tfs.stream().forEach(tf -> {
			OrderField order = orderHolder.confirmWith(tf);
			if(order == null) {
				//正常情况下不应该为空
				log.warn("没有对应的订单记录对应交易：{}", tf.toString());
				return;
			}
			feEngine.emitEvent(NorthstarEventType.ORDER, order);
		});
		if(tfs.size() > 0) {
			simGateway.save();
		}
		posHolder.updatePositionBy(tick);
		double frozenMargin = orderHolder.getFrozenMargin();
		double useMargin = posHolder.getTotalUseMargin();
		double positionProfit = posHolder.getTotalPositionProfit();
		accBuilder.setCloseProfit(accBuilder.getCloseProfit() + closeProfit);
		accBuilder.setCommission(accBuilder.getCommission() + commission);
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

		feEngine.emitEvent(NorthstarEventType.ACCOUNT, accBuilder.build());
		lastAccountEventTime = System.currentTimeMillis();
	}

	/**
	 * 入金
	 * 
	 * @param money
	 */
	protected void deposit(int money) {
		if (money < 0) {
			throw new IllegalArgumentException("入金金额不能少于零");
		}
		accBuilder.setDeposit(accBuilder.getDeposit() + money);
		refreshAccount();
		simGateway.save();
	}

	/**
	 * 出金
	 * 
	 * @param money
	 */
	protected void withdraw(int money) {
		if (money < 0) {
			throw new IllegalArgumentException("出金金额不能少于零");
		}
		accBuilder.setWithdraw(accBuilder.getWithdraw() + money);
		refreshAccount();
		simGateway.save();
	}

	/**
	 * 委托下单
	 * @param submitOrderReq
	 * @return
	 */
	protected String submitOrder(SubmitOrderReqField submitOrderReq) {
		if(submitOrderReq.getOffsetFlag() == OffsetFlagEnum.OF_Unkonwn) {
			throw new TradeException("未定义委托操作");
		}
		OrderField order;
		if(submitOrderReq.getOffsetFlag() == OffsetFlagEnum.OF_Open) {
			order = orderHolder.tryOrder(submitOrderReq, accBuilder.build());
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
		simGateway.save();
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
		simGateway.save();
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

	/**
	 * 日结算
	 */
	protected void dailySettlement() {
		// FIXME Not done
		simGateway.save();
	}

}
