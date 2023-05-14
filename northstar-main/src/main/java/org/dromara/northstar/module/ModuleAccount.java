package org.dromara.northstar.module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import org.apache.commons.codec.binary.StringUtils;
import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.exception.InsufficientException;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.ModuleAccountRuntimeDescription;
import org.dromara.northstar.common.model.ModuleDealRecord;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.common.utils.FieldUtils;
import org.dromara.northstar.data.IModuleRepository;
import org.dromara.northstar.gateway.Contract;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.strategy.IModuleAccount;
import org.slf4j.Logger;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.util.concurrent.AtomicDouble;
import com.google.protobuf.InvalidProtocolBufferException;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 模组账户
 * 一个模组账户可以对应多个真实账户信息
 * @author KevinHuangwl
 *
 */
@Slf4j
public class ModuleAccount implements IModuleAccount{

	private ModuleStateMachine stateMachine;
	
	/* gatewayId -> accCommission */
	private Map<String, AtomicDouble> accCommissionMap = new HashMap<>();	// 使用Atomic是方便累加操作
	/* gatewayId -> initBalance*/
	private Map<String, Double> initBalanceMap = new HashMap<>();
	/* gatewayId -> accDeal */
	private Map<String, AtomicInteger> accDealVolMap = new HashMap<>();		// 使用Atomic是方便累加操作
	/* gatewayId -> accCloseProfit */
	private Map<String, AtomicDouble> accCloseProfitMap = new HashMap<>();	// 使用Atomic是方便累加操作
	/* gatewayId -> maxProfit */
	private Map<String, Double> maxProfitMap = new HashMap<>();
	/* gatewayId -> maxDrawBack */
	private Map<String, Double> maxDrawBackMap = new HashMap<>();
	/* direction -> unifiedSymbol -> position */ 
	private Table<DirectionEnum, String, ModulePosition> posTable = HashBasedTable.create();
	private String tradingDay;
	
	private Logger logger;
	
	public ModuleAccount(ModuleDescription moduleDescription, ModuleRuntimeDescription moduleRtDescription, ModuleStateMachine stateMachine,
			IModuleRepository moduleRepo, IContractManager contractMgr, Logger moduleLogger) {
		this.stateMachine = stateMachine;
		this.logger = moduleLogger;
		stateMachine.setModuleAccount(this);
		BiConsumer<TradeField, TradeField> onDealCallback = (openTrade, closeTrade) -> {
			synchronized (this) {
				int factor = FieldUtils.directionFactor(openTrade.getDirection());
				ContractField contract = closeTrade.getContract();
				double profit = factor * (closeTrade.getPrice() - openTrade.getPrice()) * closeTrade.getVolume() * contract.getMultiplier();
				String gatewayId = closeTrade.getGatewayId();
				accDealVolMap.get(gatewayId).addAndGet(closeTrade.getVolume());
				accCloseProfitMap.get(gatewayId).addAndGet(profit);
				double commission = contract.getCommissionFee() > 0 ? contract.getCommissionFee() : contract.getCommissionRate() * closeTrade.getPrice() * contract.getMultiplier();
				accCommissionMap.get(gatewayId).addAndGet(commission * 2 * closeTrade.getVolume()); // 乘2代表手续费双向计算
				double maxProfit = Math.max(getMaxProfit(gatewayId), getAccCloseProfit(gatewayId) - getAccCommission(gatewayId));
				maxProfitMap.put(gatewayId, maxProfit);
				double maxDrawBack = Math.min(getMaxDrawBack(gatewayId), getAccCloseProfit(gatewayId) - getAccCommission(gatewayId) - maxProfit);
				maxDrawBackMap.put(gatewayId, maxDrawBack);
				moduleRepo.saveDealRecord(ModuleDealRecord.builder()
						.moduleName(moduleDescription.getModuleName())
						.moduleAccountId(closeTrade.getAccountId())
						.contractName(contract.getName())
						.dealProfit(profit)
						.openTrade(openTrade.toByteArray())
						.closeTrade(closeTrade.toByteArray())
						.build());
			}
		};
		
		for(ModuleAccountRuntimeDescription mard : moduleRtDescription.getAccountRuntimeDescriptionMap().values()) {
			initBalanceMap.put(mard.getAccountId(), mard.getInitBalance());
			accDealVolMap.put(mard.getAccountId(), new AtomicInteger(mard.getAccDealVolume()));
			accCloseProfitMap.put(mard.getAccountId(), new AtomicDouble(mard.getAccCloseProfit()));
			accCommissionMap.put(mard.getAccountId(), new AtomicDouble(mard.getAccCommission()));
			maxProfitMap.put(mard.getAccountId(), 0D);
			maxDrawBackMap.put(mard.getAccountId(), 0D);
			
			moduleDescription.getModuleAccountSettingsDescription().stream()
				.flatMap(mad -> mad.getBindedContracts().stream())
				.forEach(contractSimple -> {
					Contract contract = contractMgr.getContract(Identifier.of(contractSimple.getValue()));
					ContractField cf = contract.contractField();
					
					ModulePosition buyPos = new ModulePosition(mard.getAccountId(), cf, DirectionEnum.D_Buy, moduleDescription.getClosingPolicy(), onDealCallback);
					posTable.put(DirectionEnum.D_Buy, cf.getUnifiedSymbol(), buyPos);
					
					ModulePosition sellPos = new ModulePosition(mard.getAccountId(), cf, DirectionEnum.D_Sell, moduleDescription.getClosingPolicy(), onDealCallback);
					posTable.put(DirectionEnum.D_Sell, cf.getUnifiedSymbol(), sellPos);
				});
			
			mard.getPositionDescription().getNonclosedTrades().stream()
				.map(this::parseFrom)
				.filter(Objects::nonNull)
				.forEach(this::onTrade);
		}
	}
	
	private TradeField parseFrom(byte[] data) {
		try {
			return TradeField.parseFrom(data);
		} catch (InvalidProtocolBufferException e) {
			log.warn("", e);
			return null;
		}
	}

	@Override
	public void onTick(TickField tick) {
		if(!StringUtils.equals(tradingDay, tick.getTradingDay())) {
			tradingDay = tick.getTradingDay();
			tradeDayPreset();
		}
		posTable.values().stream().forEach(mp -> mp.onTick(tick));
	}

	@Override
	public void onOrder(OrderField order) {
		if(FieldUtils.isOpen(order.getOffsetFlag())) {
			posTable.get(order.getDirection(), order.getContract().getUnifiedSymbol()).onOrder(order);
		} else {
			posTable.get(FieldUtils.getOpposite(order.getDirection()), order.getContract().getUnifiedSymbol()).onOrder(order);
		}
		stateMachine.onOrder(order);
	}

	@Override
	public void onTrade(TradeField trade) {
		if(FieldUtils.isOpen(trade.getOffsetFlag())) {
			posTable.get(trade.getDirection(), trade.getContract().getUnifiedSymbol()).onTrade(trade);
		} else {
			posTable.get(FieldUtils.getOpposite(trade.getDirection()), trade.getContract().getUnifiedSymbol()).onTrade(trade);
		}
		stateMachine.onTrade(trade);
	}

	@Override
	public double getInitBalance(String gatewayId) {
		return initBalanceMap.get(gatewayId);
	}

	@Override
	public double getAccCommission(String gatewayId) {
		return accCommissionMap.get(gatewayId).get();
	}

	@Override
	public List<PositionField> getPositions(String gatewayId) {
		List<PositionField> result = new ArrayList<>();
		result.addAll(posTable.row(DirectionEnum.D_Buy).values().stream().map(ModulePosition::convertToPositionField).filter(pos -> StringUtils.equals(gatewayId, pos.getGatewayId())).toList());
		result.addAll(posTable.row(DirectionEnum.D_Sell).values().stream().map(ModulePosition::convertToPositionField).filter(pos -> StringUtils.equals(gatewayId, pos.getGatewayId())).toList());
		return result;
	}

	@Override
	public List<TradeField> getNonclosedTrades() {
		return initBalanceMap.keySet().stream()
				.map(this::getNonclosedTrades)
				.flatMap(List::stream)
				.toList();
	}
	
	@Override
	public List<TradeField> getNonclosedTrades(String gatewayId) {
		List<TradeField> result = new ArrayList<>();
		result.addAll(posTable.row(DirectionEnum.D_Buy).values().stream().flatMap(mp -> mp.getNonclosedTrades().stream()).toList());
		result.addAll(posTable.row(DirectionEnum.D_Sell).values().stream().flatMap(mp -> mp.getNonclosedTrades().stream()).toList());
		return result;
	}

	@Override
	public List<TradeField> getNonclosedTrades(String unifiedSymbol, DirectionEnum direction) {
		return posTable.row(direction).values().stream()
				.filter(mp -> StringUtils.equals(unifiedSymbol, mp.getContract().getUnifiedSymbol()))
				.flatMap(mp -> mp.getNonclosedTrades().stream())
				.toList();
	}

	@Override
	public int getNonclosedPosition(String unifiedSymbol, DirectionEnum direction) {
		return posTable.row(direction).values().stream()
				.filter(mp -> StringUtils.equals(unifiedSymbol, mp.getContract().getUnifiedSymbol()))
				.mapToInt(ModulePosition::totalVolume)
				.sum();
	}

	@Override
	public int getNonclosedNetPosition(String unifiedSymbol) {
		int longPos = getNonclosedPosition(unifiedSymbol, DirectionEnum.D_Buy);
		int shortPos = getNonclosedPosition(unifiedSymbol, DirectionEnum.D_Sell);
		return longPos - shortPos;
	}

	@Override
	public int getAccDealVolume(String gatewayId) {
		return accDealVolMap.get(gatewayId).get();
	}

	@Override
	public double getAccCloseProfit(String gatewayId) {
		return accCloseProfitMap.get(gatewayId).get();
	}

	@Override
	public double getMaxDrawBack(String gatewayId) {
		return Optional.ofNullable(maxDrawBackMap.get(gatewayId)).orElse(0D);
	}

	@Override
	public double getMaxProfit(String gatewayId) {
		return Optional.ofNullable(maxProfitMap.get(gatewayId)).orElse(0D);
	}

	@Override
	public void tradeDayPreset() {
		posTable.values().stream().forEach(ModulePosition::releaseOrder);
	}

	@Override
	public ModuleState getModuleState() {
		return stateMachine.getState();
	}
	
	/**
	 * 模组账户可用金额（近似计算）
	 * @return
	 */
	public double accountAvailableAmount(String gatewayId) {
		// 由于只有在开仓时才检查金额是否足够，因此可以忽略持仓浮盈的计算
		return getInitBalance(gatewayId) + getAccCloseProfit(gatewayId) - getAccCommission(gatewayId);
	}
	
	public void onSubmitOrder(SubmitOrderReqField submitOrder) {
		if(FieldUtils.isOpen(submitOrder.getOffsetFlag())) {
			checkIfHasSufficientAmount(submitOrder);
		} else {
			checkIfHasSufficientPosition(submitOrder);
		}
		stateMachine.onSubmitReq();
	}

	private void checkIfHasSufficientPosition(SubmitOrderReqField submitOrder) {
		ContractField contract =  submitOrder.getContract();
		int available = posTable.get(FieldUtils.getOpposite(submitOrder.getDirection()), contract.getUnifiedSymbol()).totalAvailable();
		if(available < submitOrder.getVolume()) {
			logger.warn("模组账户可用持仓为：{}，委托手数：{}", available, submitOrder.getVolume());
			throw new InsufficientException("模组账户可用持仓不足，无法平仓");
		}
	}

	private void checkIfHasSufficientAmount(SubmitOrderReqField submitOrder) {
		double margin = switch(submitOrder.getDirection()) {
		case D_Buy -> submitOrder.getContract().getLongMarginRatio();
		case D_Sell -> submitOrder.getContract().getShortMarginRatio();
		default -> throw new IllegalStateException("开仓方向不合法");
		};
		double cost = submitOrder.getPrice() * submitOrder.getVolume() * submitOrder.getContract().getMultiplier() * margin;
		if(accountAvailableAmount(submitOrder.getGatewayId()) < cost) {
			logger.warn("模组账户可用资金为：{}，开仓成本为：{}", accountAvailableAmount(submitOrder.getGatewayId()), cost);
			throw new InsufficientException("模组账户可用资金不足，无法开仓");
		}
	}

	public void onCancelOrder() {
		stateMachine.onCancelReq();
	}

}
