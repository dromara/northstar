package org.dromara.northstar.module;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.StringUtils;
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
import com.google.protobuf.InvalidProtocolBufferException;

import lombok.Getter;
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
	@Getter
	private double initBalance;
	@Getter
	private double accCommission;
	@Getter
	private int accDealVolume;
	@Getter
	private double accCloseProfit;
	@Getter
	private double maxProfit;
	@Getter
	private double maxDrawback;
	@Getter
	private double maxDrawbackPercentage;
	
	private double maxTotalBalance;
	/* direction -> unifiedSymbol -> position */ 
	private Table<DirectionEnum, String, ModulePosition> posTable = HashBasedTable.create();
	private String tradingDay;
	
	private Set<String> bindedContracts = new HashSet<>();
	
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
				accDealVolume += closeTrade.getVolume();
				accCloseProfit += profit;
				double commission = contract.getCommissionFee() > 0 ? contract.getCommissionFee() : contract.getCommissionRate() * closeTrade.getPrice() * contract.getMultiplier();
				double dealCommission = commission * 2 * closeTrade.getVolume(); // 乘2代表手续费双向计算
				accCommission += dealCommission;
				maxProfit = Math.max(maxProfit, accCloseProfit - accCommission);
				maxTotalBalance = Math.max(maxTotalBalance, initBalance + maxProfit);
				double drawback = accCloseProfit - accCommission - maxProfit;
				maxDrawback = Math.min(maxDrawback, drawback);
				maxDrawbackPercentage = Math.max(maxDrawbackPercentage, Math.abs(maxDrawback / maxTotalBalance));
				moduleRepo.saveDealRecord(ModuleDealRecord.builder()
						.moduleName(moduleDescription.getModuleName())
						.moduleAccountId(closeTrade.getAccountId())
						.contractName(contract.getName())
						.dealProfit(profit - dealCommission)
						.openTrade(openTrade.toByteArray())
						.closeTrade(closeTrade.toByteArray())
						.build());
			}
		};
		
		ModuleAccountRuntimeDescription mard = moduleRtDescription.getAccountRuntimeDescription();
		this.initBalance = moduleDescription.getInitBalance();
		this.accCloseProfit = mard.getAccCloseProfit();
		this.accCommission = mard.getAccCommission();
		this.accDealVolume = mard.getAccDealVolume();
		this.maxDrawback = mard.getMaxDrawback();
		this.maxProfit = mard.getMaxProfit();
		this.maxDrawbackPercentage = mard.getMaxDrawbackPercentage();
		moduleDescription.getModuleAccountSettingsDescription().stream()
			.flatMap(mad -> mad.getBindedContracts().stream())
			.forEach(contractSimple -> {
				Contract contract = contractMgr.getContract(Identifier.of(contractSimple.getValue()));
				ContractField cf = contract.contractField();
				
				ModulePosition buyPos = new ModulePosition(moduleRtDescription.getModuleName(), cf, DirectionEnum.D_Buy, moduleDescription.getClosingPolicy(), onDealCallback);
				posTable.put(DirectionEnum.D_Buy, cf.getUnifiedSymbol(), buyPos);
				
				ModulePosition sellPos = new ModulePosition(moduleRtDescription.getModuleName(), cf, DirectionEnum.D_Sell, moduleDescription.getClosingPolicy(), onDealCallback);
				posTable.put(DirectionEnum.D_Sell, cf.getUnifiedSymbol(), sellPos);
				
				bindedContracts.add(cf.getUnifiedSymbol());
			});
		
		mard.getPositionDescription().getNonclosedTrades().stream()
			.map(this::parseFrom)
			.filter(Objects::nonNull)
			.forEach(this::onTrade);
		
		stateMachine.updateState();
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
		if(!bindedContracts.contains(trade.getContract().getUnifiedSymbol())) {
			throw new IllegalStateException("模组账户绑定的合约与成交记录不一致：" + String.format("成交记录为[%s]", trade.getContract().getUnifiedSymbol()));
		}
		if(FieldUtils.isOpen(trade.getOffsetFlag())) {
			posTable.get(trade.getDirection(), trade.getContract().getUnifiedSymbol()).onTrade(trade);
		} else {
			posTable.get(FieldUtils.getOpposite(trade.getDirection()), trade.getContract().getUnifiedSymbol()).onTrade(trade);
		}
		stateMachine.onTrade(trade);
	}

	public List<PositionField> getPositions() {
		List<PositionField> result = new ArrayList<>();
		result.addAll(posTable.row(DirectionEnum.D_Buy).values().stream().map(ModulePosition::convertToPositionField).toList());
		result.addAll(posTable.row(DirectionEnum.D_Sell).values().stream().map(ModulePosition::convertToPositionField).toList());
		return result;
	}

	
	public List<TradeField> getNonclosedTrades() {
		List<TradeField> result = new ArrayList<>();
		result.addAll(posTable.row(DirectionEnum.D_Buy).values().stream().flatMap(mp -> mp.getNonclosedTrades().stream()).toList());
		result.addAll(posTable.row(DirectionEnum.D_Sell).values().stream().flatMap(mp -> mp.getNonclosedTrades().stream()).toList());
		return result;
	}

	public List<TradeField> getNonclosedTrades(String unifiedSymbol, DirectionEnum direction) {
		return posTable.row(direction).values().stream()
				.filter(mp -> StringUtils.equals(unifiedSymbol, mp.getContract().getUnifiedSymbol()))
				.flatMap(mp -> mp.getNonclosedTrades().stream())
				.toList();
	}

	@Override
	public int getNonclosedPosition(String unifiedSymbol, DirectionEnum direction) {
		if(!posTable.contains(direction, unifiedSymbol)) {
			return 0;
		}
		return posTable.get(direction, unifiedSymbol).totalVolume();
	}
	
	@Override
	public int getNonclosedPosition(String unifiedSymbol, DirectionEnum direction, boolean isPresentTradingDay) {
		ModulePosition mp = posTable.get(direction, unifiedSymbol);
		if(mp == null) {
			return 0;
		}
		return isPresentTradingDay ? mp.tdVolume() : mp.ydVolume();
	}
	
	@Override
	public int getNonclosedNetPosition(String unifiedSymbol) {
		int longPos = getNonclosedPosition(unifiedSymbol, DirectionEnum.D_Buy);
		int shortPos = getNonclosedPosition(unifiedSymbol, DirectionEnum.D_Sell);
		return longPos - shortPos;
	}
	
	@Override
	public double totalHoldingProfit() {
		return posTable.values().stream()
				.mapToDouble(mp -> mp.profit())
				.sum();
	}

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
	@Override
	public double availableAmount() {
		// 由于只有在开仓时才检查金额是否足够，因此可以忽略持仓浮盈的计算
		return initBalance + accCloseProfit - accCommission;
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
		if(availableAmount() < cost) {
			logger.warn("模组账户可用资金为：{}，开仓成本为：{}", availableAmount(), cost);
			throw new InsufficientException("模组账户可用资金不足，无法开仓");
		}
	}

}
