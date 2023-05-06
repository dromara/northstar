package org.dromara.northstar.support.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
		when(ctx.getModuleAccount()).thenReturn(moduleAccount);
		when(module.getModuleContext()).thenReturn(ctx);
		when(module.getName()).thenReturn("testModule");
		moduleMgr.add(module);
		
		checker = new PositionChecker(moduleMgr);
	}

	@Test
	void testDoesNotMatch() {
		PositionField accountPos = PositionField.newBuilder()
				.setContract(contract)
				.setPositionDirection(PositionDirectionEnum.PD_Long)
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
				.setPosition(2)
				.build();
	
		when(moduleAccount.getNonclosedPosition(contract.getUnifiedSymbol(), DirectionEnum.D_Buy)).thenReturn(2);
		
		assertDoesNotThrow(() -> {
			checker.checkPositionEquivalence(accountPos);
		});
	}
}
