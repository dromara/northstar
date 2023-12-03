package org.dromara.northstar.module;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.exception.InsufficientException;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.ModuleAccountRuntimeDescription;
import org.dromara.northstar.common.model.ModuleDealRecord;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Position;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.common.utils.FieldUtils;
import org.dromara.northstar.data.IModuleRepository;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.strategy.IModuleAccount;
import org.slf4j.Logger;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import lombok.Getter;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;

/**
 * 模组账户
 * 一个模组账户可以对应多个真实账户信息
 * @author KevinHuangwl
 *
 */
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
	/* direction -> contract -> position */ 
	private Table<DirectionEnum, Contract, ModulePosition> posTable = HashBasedTable.create();
	private LocalDate tradingDay;
	
	private Set<Contract> bindedContracts = new HashSet<>();
	
	private Logger logger;
	
	public ModuleAccount(ModuleDescription moduleDescription, ModuleRuntimeDescription moduleRtDescription, ModuleStateMachine stateMachine,
			IModuleRepository moduleRepo, IContractManager contractMgr, Logger moduleLogger) {
		this.stateMachine = stateMachine;
		this.logger = moduleLogger;
		stateMachine.setModuleAccount(this);
		BiConsumer<Trade, Trade> onDealCallback = (openTrade, closeTrade) -> {
			synchronized (this) {
				int factor = FieldUtils.directionFactor(openTrade.direction());
				Contract contract = closeTrade.contract();
				double profit = factor * (closeTrade.price() - openTrade.price()) * closeTrade.volume() * contract.multiplier();
				accDealVolume += closeTrade.volume();
				accCloseProfit += profit;
				ContractDefinition cd = contract.contractDefinition();
				double commission = cd.commissionFee() > 0 ? cd.commissionFee() : cd.commissionRate() * closeTrade.price() * contract.multiplier();
				double dealCommission = commission * 2 * closeTrade.volume(); // 乘2代表手续费双向计算
				accCommission += dealCommission;
				maxProfit = Math.max(maxProfit, accCloseProfit - accCommission);
				maxTotalBalance = Math.max(maxTotalBalance, initBalance + maxProfit);
				double drawback = accCloseProfit - accCommission - maxProfit;
				maxDrawback = Math.min(maxDrawback, drawback);
				maxDrawbackPercentage = Math.max(maxDrawbackPercentage, Math.abs(maxDrawback / maxTotalBalance));
				moduleRepo.saveDealRecord(ModuleDealRecord.builder()
						.moduleName(moduleDescription.getModuleName())
						.moduleAccountId(closeTrade.gatewayId())
						.contractName(contract.name())
						.dealProfit(profit - dealCommission)
						.openTrade(openTrade)
						.closeTrade(closeTrade)
						.build());
			}
		};
		
		ModuleAccountRuntimeDescription mard = moduleRtDescription.getModuleAccountRuntime();
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
				IContract contract = contractMgr.getContract(Identifier.of(contractSimple.getValue()));
				Contract cf = contract.contract();
				
				ModulePosition buyPos = new ModulePosition(moduleRtDescription.getModuleName(), cf, DirectionEnum.D_Buy, moduleDescription.getClosingPolicy(), onDealCallback);
				posTable.put(DirectionEnum.D_Buy, cf, buyPos);
				
				ModulePosition sellPos = new ModulePosition(moduleRtDescription.getModuleName(), cf, DirectionEnum.D_Sell, moduleDescription.getClosingPolicy(), onDealCallback);
				posTable.put(DirectionEnum.D_Sell, cf, sellPos);
				
				bindedContracts.add(cf);
			});
		
		mard.getPositionDescription().getNonclosedTrades().forEach(this::onTrade);
		
		stateMachine.updateState();
	}
	
	@Override
	public void onTick(Tick tick) {
		if(Objects.isNull(tradingDay) || !tradingDay.isEqual(tick.tradingDay())) {
			tradingDay = tick.tradingDay();
			tradeDayPreset();
		}
		posTable.values().stream().forEach(mp -> mp.onTick(tick));
	}

	@Override
	public void onOrder(Order order) {
		if(FieldUtils.isOpen(order.offsetFlag())) {
			posTable.get(order.direction(), order.contract()).onOrder(order);
		} else {
			posTable.get(FieldUtils.getOpposite(order.direction()), order.contract()).onOrder(order);
		}
		stateMachine.onOrder(order);
	}

	@Override
	public void onTrade(Trade trade) {
		if(!bindedContracts.contains(trade.contract())) {
			throw new IllegalStateException("模组账户绑定的合约与成交记录不一致：" + String.format("成交记录为[%s]", trade.contract().unifiedSymbol()));
		}
		if(FieldUtils.isOpen(trade.offsetFlag())) {
			posTable.get(trade.direction(), trade.contract()).onTrade(trade);
		} else {
			posTable.get(FieldUtils.getOpposite(trade.direction()), trade.contract()).onTrade(trade);
		}
		stateMachine.onTrade(trade);
	}

	public List<Position> getPositions() {
		List<Position> result = new ArrayList<>();
		result.addAll(posTable.row(DirectionEnum.D_Buy).values().stream().map(ModulePosition::convertToPosition).toList());
		result.addAll(posTable.row(DirectionEnum.D_Sell).values().stream().map(ModulePosition::convertToPosition).toList());
		return result;
	}

	
	public List<Trade> getNonclosedTrades() {
		List<Trade> result = new ArrayList<>();
		result.addAll(posTable.row(DirectionEnum.D_Buy).values().stream().flatMap(mp -> mp.getNonclosedTrades().stream()).toList());
		result.addAll(posTable.row(DirectionEnum.D_Sell).values().stream().flatMap(mp -> mp.getNonclosedTrades().stream()).toList());
		return result;
	}

	@Override
	public int getNonclosedPosition(Contract contract, DirectionEnum direction) {
		if(!posTable.contains(direction, contract)) {
			return 0;
		}
		return posTable.get(direction, contract).totalVolume();
	}
	
	@Override
	public int getNonclosedPosition(Contract contract, DirectionEnum direction, boolean isPresentTradingDay) {
		ModulePosition mp = posTable.get(direction, contract);
		if(mp == null) {
			return 0;
		}
		return isPresentTradingDay ? mp.tdVolume() : mp.ydVolume();
	}
	
	@Override
	public int getNonclosedNetPosition(Contract contract) {
		int longPos = getNonclosedPosition(contract, DirectionEnum.D_Buy);
		int shortPos = getNonclosedPosition(contract, DirectionEnum.D_Sell);
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
		return initBalance + accCloseProfit - accCommission - holdingMargin();
	}
	
	// 持仓占用
	private double holdingMargin() {
		return posTable.values().stream().mapToDouble(ModulePosition::totalMargin).sum();
	}
	
	public void onSubmitOrder(SubmitOrderReq submitOrder) {
		if(FieldUtils.isOpen(submitOrder.offsetFlag())) {
			checkIfHasSufficientAmount(submitOrder);
		} else {
			checkIfHasSufficientPosition(submitOrder);
		}
		stateMachine.onSubmitReq();
	}

	private void checkIfHasSufficientPosition(SubmitOrderReq submitOrder) {
		Contract contract =  submitOrder.contract();
		int available = posTable.get(FieldUtils.getOpposite(submitOrder.direction()), contract).totalAvailable();
		if(available < submitOrder.volume()) {
			logger.warn("模组账户可用持仓为：{}，委托手数：{}", available, submitOrder.volume());
			throw new InsufficientException("模组账户可用持仓不足，无法平仓");
		}
	}

	private void checkIfHasSufficientAmount(SubmitOrderReq submitOrder) {
		double margin = switch(submitOrder.direction()) {
		case D_Buy -> submitOrder.contract().longMarginRatio();
		case D_Sell -> submitOrder.contract().shortMarginRatio();
		default -> throw new IllegalStateException("开仓方向不合法");
		};
		double cost = submitOrder.price() * submitOrder.volume() * submitOrder.contract().multiplier() * margin;
		if(availableAmount() < cost) {
			logger.warn("模组账户可用资金为：{}，开仓成本为：{}", availableAmount(), cost);
			throw new InsufficientException("模组账户可用资金不足，无法开仓");
		}
	}

}
