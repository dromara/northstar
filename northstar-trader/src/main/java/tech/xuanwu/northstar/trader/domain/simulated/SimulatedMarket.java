package tech.xuanwu.northstar.trader.domain.simulated;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.StringUtils;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.constant.CommonConstant;
import tech.xuanwu.northstar.exception.TradeException;
import tech.xuanwu.northstar.gateway.FastEventEngine;
import tech.xuanwu.northstar.gateway.FastEventEngine.EventType;
import tech.xuanwu.northstar.gateway.FastEventEngine.FastEvent;
import tech.xuanwu.northstar.gateway.FastEventEngine.FastEventHandler;
import tech.xuanwu.northstar.persistance.po.Account;
import tech.xuanwu.northstar.persistance.po.Position;
import xyz.redtorch.pb.CoreEnum.CommonStatusEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.NoticeField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 一个SimulatedMarket对象对应一个网关的模拟计算
 * 
 * @author kevinhuangwl
 *
 */
@Slf4j
public class SimulatedMarket implements FastEventHandler {
	
	/*账户信息*/
	private GwAccount gwAccount;
	/*持仓信息*/
	private GwPositions gwPositions;
	/*订单信息*/
	private GwOrders gwOrders;
	
	volatile String tradingDay;
	volatile String actionDay;
	
	private volatile String lastTradingDay = LocalDate.now().format(CommonConstant.D_FORMAT_INT_FORMATTER);
	private volatile long lastReportTime;

	private FastEventEngine feEngine;
	private String gatewayId;
	private Map<String, ContractField> contractMap;
	
	private ScheduledExecutorService execService = Executors.newScheduledThreadPool(1);
	
	private final int REPORT_INTERVAL = 10000;
	
	private final String DATA_GATEWAY;
	
	private volatile boolean hasInit;
	
	public SimulatedMarket(String gatewayId, String marketDataGatewayId, FastEventEngine feEngine, Map<String, ContractField> contractMap) {
		this.DATA_GATEWAY = marketDataGatewayId;
		this.feEngine = feEngine;
		this.feEngine.addHandler(this);
		this.gatewayId = gatewayId;
		this.contractMap = contractMap;
		
	}
	
	public Account init(Account accountPO) {
		log.info("初始化模拟市场");
		execService.scheduleAtFixedRate(() -> {
			if(System.currentTimeMillis() - lastReportTime < REPORT_INTERVAL) {
				// 当有行情刷新触发回报时，无需要定时回报
				return;
			}
			gwPositions.getLongPositionMap().forEach((k,v) -> feEngine.emitEvent(EventType.POSITION, "", v));
			gwPositions.getShortPositionMap().forEach((k,v) -> feEngine.emitEvent(EventType.POSITION, "", v));
			feEngine.emitEvent(EventType.ACCOUNT, "", gwAccount.getAccount());
		}, 1000, REPORT_INTERVAL, TimeUnit.MILLISECONDS);
		
		if(accountPO == null) {
			gwAccount = new GwAccount(gatewayId);
			gwPositions = new GwPositions();
			log.info("创建新账户");
		} else {
			gwAccount = new GwAccount(accountPO.convertTo());
			Map<String, Position> positionMap = accountPO.getPositionMap();
			ConcurrentHashMap<String, PositionField> longPositionMap = new ConcurrentHashMap<>(positionMap.size());
			ConcurrentHashMap<String, PositionField> shortPositionMap = new ConcurrentHashMap<>(positionMap.size());
			positionMap.forEach((k, v) -> {
				ContractField contract = contractMap.get(v.getContractUnifiedSymbol());
				if(v.getPositionDirection() == PositionDirectionEnum.PD_Long) {
					longPositionMap.put(contract.getUnifiedSymbol(), v.convertTo(contract));
				}
				if(v.getPositionDirection() == PositionDirectionEnum.PD_Short) {
					shortPositionMap.put(contract.getUnifiedSymbol(), v.convertTo(contract));
				}
			});
			gwPositions = new GwPositions(longPositionMap, shortPositionMap);
			log.info("加载旧账户");
		}
		
		gwOrders = new GwOrders();
		
		gwAccount.setGwOrders(gwOrders);
		gwAccount.setGwPositions(gwPositions);
		
		gwPositions.setContractMap(contractMap);
		gwPositions.setGwAccount(gwAccount);
		
		gwOrders.setContractMap(contractMap);
		gwOrders.setGwAccount(gwAccount);
		gwOrders.setGwPositions(gwPositions);
		
		hasInit = true;
		log.info("当前账户：{}", gwAccount.getAccount());
		return Account.convertFrom(gwAccount.getAccount());
	}
	
	/**
	 * 行情更新
	 * 
	 */
	@Override
	public void onEvent(FastEvent event, long sequence, boolean endOfBatch) throws Exception {
		if (!hasInit || event.getEventType() != EventType.TICK) {
			return;
		}

		TickField tick = (TickField) event.getObj();
		if(!StringUtils.equals(DATA_GATEWAY, tick.getGatewayId())) {
			return;
		}
		tradingDay = tick.getTradingDay();
		actionDay = tick.getActionDay();

		if(!StringUtils.equals(tradingDay, lastTradingDay)) {
			lastTradingDay = tradingDay;
			proceedDailySettlement();
		}
		
		List<TradeField> tradeList = gwOrders.tryDeal(tick);
		if(tradeList.size() > 0) {
			tradeList.forEach(t -> {
				feEngine.emitEvent(EventType.TRADE, "", t);
				OrderField order = gwOrders.handleDeal(t);
				feEngine.emitEvent(EventType.ORDER, "", order);
				OffsetFlagEnum kpType = t.getOffsetFlag();
				if(kpType == OffsetFlagEnum.OF_Open) {
					feEngine.emitEvent(EventType.POSITION, "", gwPositions.addPosition(t));
				}else {
					gwPositions.unfrozenPosition(order);
					feEngine.emitEvent(EventType.POSITION, "", gwPositions.reducePosition(t));
				}
			});
		}

		PositionField lp = gwPositions.updateLongPositionBy(tick);
		if(lp != null) {
			feEngine.emitEvent(EventType.POSITION, "", lp);
		}
		
		PositionField sp = gwPositions.updateShortPositionBy(tick);
		if(sp != null) {
			feEngine.emitEvent(EventType.POSITION, "", sp);
		}
		
		if(System.currentTimeMillis() - lastReportTime < 1000) {
			// 减少账户更新频率
			return;
		}
		feEngine.emitEvent(EventType.ACCOUNT, "", gwAccount.getAccount());

		lastReportTime = System.currentTimeMillis();
	}
	
	/**
	 * 委托下单
	 * 
	 * @param submitOrderReqReq
	 */
	public synchronized String submitOrderReq(SubmitOrderReqField submitOrderReq) {
		if (submitOrderReq.getOffsetFlag() == OffsetFlagEnum.OF_Unkonwn) {
			throw new IllegalStateException("委托单开平仓状态异常：未知的开平仓操作");
		}
		
		OrderField order = gwOrders.submitOrder(submitOrderReq);
		feEngine.emitEvent(EventType.ORDER, "", order);
		
		if(order.getOrderStatus() == OrderStatusEnum.OS_Rejected) {
			log.warn("【{}】交易异常：{}", submitOrderReq.getGatewayId(), order.getStatusMsg());
			throw new TradeException(order.getStatusMsg());
		}
		
		// 平仓委托时，需要冻结仓位
		if(order.getOffsetFlag() != OffsetFlagEnum.OF_Open && order.getOrderStatus() == OrderStatusEnum.OS_Touched) {
			feEngine.emitEvent(EventType.POSITION, "", gwPositions.frozenPosition(order));
		}
		
		AccountField account = gwAccount.getAccount();
		feEngine.emitEvent(EventType.ACCOUNT, "", account);

		return order.getOriginOrderId();
	}

	/**
	 * 撤单
	 * 
	 * @param cancelOrderReq
	 */
	public synchronized void cancelOrder(CancelOrderReqField cancelOrderReq) {
		OrderField order = gwOrders.cancelOrder(cancelOrderReq);
		if(order != null) {
			feEngine.emitEvent(EventType.ORDER, "", order);
			if(order.getOffsetFlag() != OffsetFlagEnum.OF_Open && order.getOffsetFlag() != OffsetFlagEnum.OF_Unkonwn 
					&& order.getOrderStatus() == OrderStatusEnum.OS_Canceled) {
				feEngine.emitEvent(EventType.POSITION, "", gwPositions.unfrozenPosition(order));
			}
			return;
		}
		
		NoticeField notice = NoticeField.newBuilder()
				.setContent("挂单已撤单")
				.setTimestamp(System.currentTimeMillis())
				.setStatus(CommonStatusEnum.COMS_WARN)
				.build();
		feEngine.emitEvent(EventType.NOTICE, "", notice);
	}

	/**
	 * 进行日结算
	 */
	public void proceedDailySettlement() {
		// TODO 稍后实现
	}
	
	/**
	 * 保存账户信息
	 */
	public boolean save() {
		
		return false;
	}
	
	/**
	 * 读取账户信息
	 */
	public boolean load() {
		return false;
	}
	
	/**
	 * 入金
	 * @param money
	 */
	public void deposit(int money) {
		feEngine.emitEvent(EventType.BALANCE_CHANGE, "", gwAccount.deposit(money));
	}
	
	/**
	 * 出金
	 * @param money
	 */
	public void withdraw(int money) {
		feEngine.emitEvent(EventType.BALANCE_CHANGE, "", gwAccount.withdraw(money));
	}
}
