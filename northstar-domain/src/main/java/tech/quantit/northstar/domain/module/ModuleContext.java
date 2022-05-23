package tech.quantit.northstar.domain.module;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import tech.quantit.northstar.common.constant.Constants;
import tech.quantit.northstar.common.constant.SignalOperation;
import tech.quantit.northstar.common.exception.NoSuchElementException;
import tech.quantit.northstar.common.exception.TradeException;
import tech.quantit.northstar.common.model.ModuleAccountRuntimeDescription;
import tech.quantit.northstar.common.model.ModuleDealRecord;
import tech.quantit.northstar.common.model.ModuleRuntimeDescription;
import tech.quantit.northstar.common.model.ModulePositionDescription;
import tech.quantit.northstar.common.utils.FieldUtils;
import tech.quantit.northstar.common.utils.OrderUtils;
import tech.quantit.northstar.gateway.api.TradeGateway;
import tech.quantit.northstar.strategy.api.ClosingStrategy;
import tech.quantit.northstar.strategy.api.IModule;
import tech.quantit.northstar.strategy.api.IModuleAccountStore;
import tech.quantit.northstar.strategy.api.IModuleContext;
import tech.quantit.northstar.strategy.api.TradeStrategy;
import tech.quantit.northstar.strategy.api.constant.PriceType;
import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.ForceCloseReasonEnum;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;
import xyz.redtorch.pb.CoreEnum.VolumeConditionEnum;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 模组上下文
 * @author KevinHuangwl
 *
 */
public class ModuleContext implements IModuleContext{
	
	protected TradeStrategy tradeStrategy;
	
	protected IModuleAccountStore accStore;
	
	protected ClosingStrategy closingStrategy;
	
	protected IModule module;
	
	/* originOrderId -> order */
	private Map<String, OrderField> orderMap = new HashMap<>();
	
	/* contract -> gateway */
	private Map<ContractField, TradeGateway> gatewayMap = new HashMap<>();
	
	/* unifiedSymbol -> contract */
	private Map<String, ContractField> contractMap = new HashMap<>();
	
	/* unifiedSymbol -> barMerger */
	private Map<String, BarMerger> contractBarMergerMap = new HashMap<>();
	
	/* unifiedSymbol -> tick */
	private Map<String, TickField> tickMap = new HashMap<>();
	
	private String tradingDay = "";
	
	private Consumer<BarField> barMergingCallback;
	
	private int numOfMinsPerBar;
	
	private DealCollector dealCollector;
	
	private Consumer<ModuleRuntimeDescription> onRuntimeChangeCallback;
	
	private Consumer<ModuleDealRecord> onDealCallback; 
	
	public ModuleContext(TradeStrategy tradeStrategy, IModuleAccountStore accStore, ClosingStrategy closingStrategy, int numOfMinsPerBar, 
			DealCollector dealCollector, Consumer<ModuleRuntimeDescription> onRuntimeChangeCallback, Consumer<ModuleDealRecord> onDealCallback) {
		this.tradeStrategy = tradeStrategy;
		this.accStore = accStore;
		this.closingStrategy = closingStrategy;
		this.numOfMinsPerBar = numOfMinsPerBar;
		this.dealCollector = dealCollector;
		this.onRuntimeChangeCallback = onRuntimeChangeCallback;
		this.onDealCallback = onDealCallback;
		this.barMergingCallback = bar -> tradeStrategy.onBar(bar, module.isEnabled());
		tradeStrategy.setContext(this);
	}

	@Override
	public ModuleRuntimeDescription getRuntimeDescription() {
		Map<String, ModuleAccountRuntimeDescription> accMap = new HashMap<>();
		for(TradeGateway gateway : gatewayMap.values()) {
			String gatewayId = gateway.getGatewaySetting().getGatewayId();
			if(accMap.containsKey(gatewayId)) {
				continue;
			}
			ModulePositionDescription posDescription = ModulePositionDescription.builder()
					.logicalPositions(accStore.getPositions(gatewayId).stream().map(PositionField::toByteArray).toList())
					.uncloseTrades(accStore.getUncloseTrades(gatewayId).stream().map(TradeField::toByteArray).toList())
					.build();
			
			ModuleAccountRuntimeDescription accDescription = ModuleAccountRuntimeDescription.builder()
					.accountId(gatewayId)
					.initBalance(accStore.getInitBalance(gatewayId))
					.preBalance(accStore.getPreBalance(gatewayId))
					.accCloseProfit(accStore.getAccCloseProfit(gatewayId))
					.accDealVolume(accStore.getAccDealVolume(gatewayId))
					.accCommission(accStore.getAccCommission(gatewayId))
					.positionDescription(posDescription)
					.build();
			accMap.put(gatewayId, accDescription);
		}
		return ModuleRuntimeDescription.builder()
				.moduleName(module.getName())
				.enabled(module.isEnabled())
				.moduleState(accStore.getModuleState())
				.dataState(tradeStrategy.getComputedState())
				.accountRuntimeDescriptionMap(accMap)
				.build();
	}

	@Override
	public String submitOrderReq(ContractField contract, SignalOperation operation,
			PriceType priceType, int volume, double price) {
		if(!gatewayMap.containsKey(contract)) {
			throw new NoSuchElementException(String.format("找不到合约 [%s] 对应网关", contract.getUnifiedSymbol()));
		}
		String id = UUID.randomUUID().toString();
		String gatewayId = gatewayMap.get(contract).getGatewaySetting().getGatewayId();
		PositionField pf = null;
		for(PositionField pos : accStore.getPositions(gatewayId)) {
			boolean isOppositeDir = (operation.isBuy() && FieldUtils.isShort(pos.getPositionDirection()) 
					|| operation.isSell() && FieldUtils.isLong(pos.getPositionDirection()));
			if(operation.isClose() && pos.getContract().equals(contract) && isOppositeDir) {
				pf = pos;
			}
		}
		if(pf == null && operation.isClose()) {
			throw new IllegalStateException("没有找到对应的持仓进行操作");
		}
		return submitOrderReq(SubmitOrderReqField.newBuilder()
				.setOriginOrderId(id)
				.setContract(contract)
				.setGatewayId(gatewayId)
				.setDirection(OrderUtils.resolveDirection(operation))
				.setOffsetFlag(closingStrategy.resolveOperation(operation, pf))
				.setPrice(price)
				.setVolume(volume)		//	当信号交易量大于零时，优先使用信号交易量
				.setHedgeFlag(HedgeFlagEnum.HF_Speculation)
				.setTimeCondition(priceType == PriceType.ANY_PRICE ? TimeConditionEnum.TC_IOC : TimeConditionEnum.TC_GFD)
				.setOrderPriceType(priceType == PriceType.ANY_PRICE ? OrderPriceTypeEnum.OPT_AnyPrice : OrderPriceTypeEnum.OPT_LimitPrice)
				.setVolumeCondition(VolumeConditionEnum.VC_AV)
				.setForceCloseReason(ForceCloseReasonEnum.FCR_NotForceClose)
				.setContingentCondition(ContingentConditionEnum.CC_Immediately)
				.setMinVolume(1)
				.build());
	}

	private String submitOrderReq(SubmitOrderReqField orderReq) {
		if(FieldUtils.isOpen(orderReq.getOffsetFlag())) {
			checkAmount(orderReq);
		}
		ContractField contract = orderReq.getContract();
		TradeGateway gateway = gatewayMap.get(contract);
		gateway.submitOrder(orderReq);
		OrderField placeholder = OrderField.newBuilder()
				.setGatewayId(gateway.getGatewaySetting().getGatewayId())
				.setContract(contract)
				.setOriginOrderId(orderReq.getOriginOrderId())
				.build();
		orderMap.put(orderReq.getOriginOrderId(), placeholder);
		accStore.onSubmitOrder(orderReq);
		return orderReq.getOriginOrderId();
	}
	
	private void checkAmount(SubmitOrderReqField orderReq) {
		double orderPrice = orderReq.getOrderPriceType() == OrderPriceTypeEnum.OPT_AnyPrice ? tickMap.get(orderReq.getContract().getUnifiedSymbol()).getLastPrice() : orderReq.getPrice();
		double extMargin = orderReq.getVolume() * orderPrice * orderReq.getContract().getMultiplier() * FieldUtils.marginRatio(orderReq.getContract(), orderReq.getDirection());
		double preBalance = accStore.getPreBalance(orderReq.getGatewayId());
		if(preBalance < extMargin) {
			throw new TradeException(String.format("模组可用资金 [%s] 小于开仓保证金 [%s]", preBalance, extMargin));
		}
	}

	@Override
	public void cancelOrder(String originOrderId) {
		if(!orderMap.containsKey(originOrderId)) {
			throw new NoSuchElementException("找不到订单：" + originOrderId);
		}
		ContractField contract = orderMap.get(originOrderId).getContract();
		TradeGateway gateway = gatewayMap.get(contract);
		CancelOrderReqField cancelReq = CancelOrderReqField.newBuilder()
				.setGatewayId(gateway.getGatewaySetting().getGatewayId())
				.setOriginOrderId(originOrderId)
				.build();
		accStore.onCancelOrder(cancelReq);
		gateway.cancelOrder(cancelReq);
	}

	/* 此处收到的TICK数据是所有订阅的数据，需要过滤 */
	@Override
	public void onTick(TickField tick) {
		if(!contractBarMergerMap.containsKey(tick.getUnifiedSymbol())) {
			return;
		}
		if(!StringUtils.equals(tradingDay, tick.getTradingDay())) {
			tradingDay = tick.getTradingDay();
		}
		accStore.onTick(tick);
		tickMap.put(tick.getUnifiedSymbol(), tick);
		tradeStrategy.onTick(tick, module.isEnabled());
	}
	
	/* 此处收到的BAR数据是所有订阅的数据，需要过滤 */
	@Override
	public void onBar(BarField bar) {
		if(!contractBarMergerMap.containsKey(bar.getUnifiedSymbol())) {
			return;
		}
		contractBarMergerMap.get(bar.getUnifiedSymbol()).updateBar(bar);
	}
	
	/* 此处收到的ORDER数据是所有订单回报，需要过滤 */
	@Override
	public void onOrder(OrderField order) {
		if(!orderMap.containsKey(order.getOriginOrderId())) {
			return;
		}
		if(!OrderUtils.isValidOrder(order)) {
			orderMap.remove(order.getOriginOrderId());
		}
		accStore.onOrder(order);
		tradeStrategy.onOrder(order);
	}

	/* 此处收到的TRADE数据是所有成交回报，需要过滤 */
	@Override
	public void onTrade(TradeField trade) {
		if(!orderMap.containsKey(trade.getOriginOrderId()) && !StringUtils.equals(trade.getOriginOrderId(), Constants.MOCK_ORDER_ID)) {
			return;
		}
		if(orderMap.containsKey(trade.getOriginOrderId())) {
			orderMap.remove(trade.getOriginOrderId());
		}
		accStore.onTrade(trade);
		tradeStrategy.onTrade(trade);
		onRuntimeChangeCallback.accept(getRuntimeDescription());
		dealCollector.onTrade(trade).ifPresent(list -> list.stream().forEach(this.onDealCallback::accept));
	}

	@Override
	public TradeStrategy getTradeStrategy() {
		return tradeStrategy;
	}

	@Override
	public void disabledModule() {
		module.setEnabled(false);
	}

	@Override
	public void setModule(IModule module) {
		this.module = module;
	}

	@Override
	public String getModuleName() {
		return module.getName();
	}

	@Override
	public void bindGatewayContracts(TradeGateway gateway, List<ContractField> contracts) {
		for(ContractField c : contracts) {			
			gatewayMap.put(c, gateway);
			contractMap.put(c.getUnifiedSymbol(), c);
			contractBarMergerMap.put(c.getUnifiedSymbol(), new BarMerger(numOfMinsPerBar, c, barMergingCallback));
		}
	}

	@Override
	public ContractField getContract(String unifiedSymbol) {
		if(!contractMap.containsKey(unifiedSymbol)) {
			throw new NoSuchElementException("找不到合约：" + unifiedSymbol);
		}
		return contractMap.get(unifiedSymbol);
	}

}
