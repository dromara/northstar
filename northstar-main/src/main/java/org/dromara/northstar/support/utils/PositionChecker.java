package org.dromara.northstar.support.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.core.Position;
import org.dromara.northstar.common.utils.FieldUtils;
import org.dromara.northstar.module.ModuleManager;
import org.dromara.northstar.strategy.IModule;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;

/**
 * 持仓检查器
 * @author KevinHuangwl
 *
 */
@Slf4j
public class PositionChecker {
	
	private ModuleManager moduleMgr;

	public PositionChecker(ModuleManager moduleMgr) {
		this.moduleMgr = moduleMgr;
	}
	
	/**
	 * 检查持仓相等
	 * @param position	某合约某个方向的物理持仓
	 */
	public void checkPositionEquivalence(Position position) {
		DirectionEnum direction = switch(position.positionDirection()) {
		case PD_Long -> DirectionEnum.D_Buy;
		case PD_Short -> DirectionEnum.D_Sell;
		default -> throw new IllegalArgumentException("Unexpected value: " + position.positionDirection());
		};
		List<IModule> modules = moduleMgr.findAll()
									.stream()
									.filter(m -> hasLinkageBetweenModuleAndPosition(m, position))
									.filter(m -> m.getModuleContext().getModuleDescription().getBindedContracts()
											.stream()
											.map(ContractSimpleInfo::getUnifiedSymbol)
											.collect(Collectors.toSet()).contains(position.contract().unifiedSymbol()))
									.toList();
		double totalPosition = modules.stream()
									.map(m -> m.getModuleContext().getModuleAccount().getPosition(FieldUtils.openPositionDirOf(direction), position.contract()))
									.filter(Optional::isPresent)
									.map(Optional::get)
									.mapToDouble(p -> p.position())
									.sum();
		if(totalPosition != position.position()) {
			log.warn("{} {}头 实际持仓数：{}", position.contract().name(), FieldUtils.chn(direction), position.position());
			modules
			.stream()
			.filter(m -> m.getModuleContext().getModuleAccount().getPosition(FieldUtils.openPositionDirOf(direction), position.contract()).isPresent())
			.forEach(m -> 
				log.warn("模组 [{}] {}头 持仓为：{}", m.getModuleContext().getModuleDescription().getName(), FieldUtils.chn(direction), 
						m.getModuleContext().getModuleAccount().getPosition(FieldUtils.openPositionDirOf(direction), position.contract()).get().position())
			);
			throw new IllegalStateException(String.format("[%s %s %s头] 逻辑持仓与实际持仓不一致。逻辑持仓数：%s，实际持仓数：%s", 
					position.gatewayId(), position.contract().name(), FieldUtils.chn(direction), 
					BigDecimal.valueOf(totalPosition).setScale(position.contract().quantityPrecision(), RoundingMode.HALF_UP),
					BigDecimal.valueOf(position.position()).setScale(position.contract().quantityPrecision(), RoundingMode.HALF_UP)));
		}
	}
	
	private boolean hasLinkageBetweenModuleAndPosition(IModule module, Position position) {
		return module.getModuleContext().getModuleDescription().getBindedAccounts()
				.stream()
				.collect(Collectors.toSet()).contains(position.gatewayId());
	}
}
