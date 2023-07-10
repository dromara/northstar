package org.dromara.northstar.support.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.ModuleAccountDescription;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.module.ModuleManager;
import org.dromara.northstar.strategy.IModule;
import org.dromara.northstar.strategy.IModuleAccount;
import org.dromara.northstar.strategy.IModuleContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.PositionField;

class PositionCheckerTest {
	
	IModuleAccount moduleAccount = mock(IModuleAccount.class);
	IModuleContext ctx = mock(IModuleContext.class);
	IModule module = mock(IModule.class);
	
	ModuleManager moduleMgr = new ModuleManager();
	PositionChecker checker;
	
	TestFieldFactory fieldFactory = new TestFieldFactory("test");
	
	ContractField contract = fieldFactory.makeContract("rb2310");
	
	@BeforeEach
	void prepare() {
		ModuleAccountDescription mad = ModuleAccountDescription.builder()
				.accountGatewayId("testGateway")
				.bindedContracts(List.of(ContractSimpleInfo.builder().unifiedSymbol(contract.getUnifiedSymbol()).build()))
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
		PositionField accountPos = PositionField.newBuilder()
				.setContract(contract)
				.setPositionDirection(PositionDirectionEnum.PD_Long)
				.setGatewayId("testGateway")
				.build();
	
		when(moduleAccount.getNonclosedPosition(contract.getUnifiedSymbol(), DirectionEnum.D_Buy)).thenReturn(2);
		
		assertThrows(IllegalStateException.class, () -> {
			checker.checkPositionEquivalence(accountPos);
		});
	}

	@Test
	void testDoesNotMatch2() {
		PositionField accountPos = PositionField.newBuilder()
				.setContract(contract)
				.setPositionDirection(PositionDirectionEnum.PD_Long)
				.setGatewayId("testGateway")
				.setPosition(2)
				.build();
	
		when(moduleAccount.getNonclosedPosition(contract.getUnifiedSymbol(), DirectionEnum.D_Sell)).thenReturn(2);
		
		assertThrows(IllegalStateException.class, () -> {
			checker.checkPositionEquivalence(accountPos);
		});
	}
	
	@Test
	void testMatch() {
		PositionField accountPos = PositionField.newBuilder()
				.setContract(contract)
				.setPositionDirection(PositionDirectionEnum.PD_Long)
				.setGatewayId("testGateway")
				.setPosition(2)
				.build();
	
		when(moduleAccount.getNonclosedPosition(contract.getUnifiedSymbol(), DirectionEnum.D_Buy)).thenReturn(2);
		
		assertDoesNotThrow(() -> {
			checker.checkPositionEquivalence(accountPos);
		});
	}
}
