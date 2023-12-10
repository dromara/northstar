package org.dromara.northstar.support.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.ModuleAccountDescription;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Position;
import org.dromara.northstar.module.ModuleManager;
import org.dromara.northstar.strategy.IModule;
import org.dromara.northstar.strategy.IModuleAccount;
import org.dromara.northstar.strategy.IModuleContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

class PositionCheckerTest {

	IModuleAccount moduleAccount = mock(IModuleAccount.class);
	IModuleContext ctx = mock(IModuleContext.class);
	IModule module = mock(IModule.class);

	ModuleManager moduleMgr = new ModuleManager();
	PositionChecker checker;

	Contract contract = Contract.builder().unifiedSymbol("rb2401").build();

	@BeforeEach
	void prepare() {
		ModuleAccountDescription mad = ModuleAccountDescription.builder()
				.accountGatewayId("testGateway")
				.bindedContracts(List.of(ContractSimpleInfo.builder().unifiedSymbol(contract.unifiedSymbol()).build()))
				.build();
		ModuleDescription md = ModuleDescription.builder()
				.moduleAccountSettingsDescription(List.of(mad))
				.build();
		when(ctx.getModuleAccount()).thenReturn(moduleAccount);
		when(module.getModuleContext()).thenReturn(ctx);
		when(module.getName()).thenReturn("testModule");
		when(module.getModuleDescription()).thenReturn(md);
		moduleMgr.add(module);

		checker = new PositionChecker(moduleMgr);
	}

	@Test
	void testDoesNotMatch() {
		Position accountPos = Position.builder()
				.contract(contract)
				.positionDirection(PositionDirectionEnum.PD_Long)
				.gatewayId("testGateway")
				.build();

		when(moduleAccount.getNonclosedPosition(contract, DirectionEnum.D_Buy)).thenReturn(2);

		assertThrows(IllegalStateException.class, () -> {
			checker.checkPositionEquivalence(accountPos);
		});
	}

	@Test
	void testDoesNotMatch2() {
		Position accountPos = Position.builder()
				.contract(contract)
				.positionDirection(PositionDirectionEnum.PD_Long)
				.gatewayId("testGateway")
				.position(2)
				.build();

		when(moduleAccount.getNonclosedPosition(contract, DirectionEnum.D_Sell)).thenReturn(2);

		assertThrows(IllegalStateException.class, () -> {
			checker.checkPositionEquivalence(accountPos);
		});
	}

	@Test
	void testMatch() {
		Position accountPos = Position.builder()
				.contract(contract)
				.positionDirection(PositionDirectionEnum.PD_Long)
				.gatewayId("testGateway")
				.position(2)
				.build();

		when(moduleAccount.getNonclosedPosition(contract, DirectionEnum.D_Buy)).thenReturn(2);

		assertDoesNotThrow(() -> {
			checker.checkPositionEquivalence(accountPos);
		});
	}
}
