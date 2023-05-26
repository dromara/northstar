package org.dromara.northstar.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.dromara.northstar.account.AccountManager;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.strategy.IModuleContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TradeModuleTest {
    private TradeModule tradeModule;
    private ModuleDescription moduleDescription;
    private IModuleContext moduleContext;
    private AccountManager accountManager;
    private IContractManager contractManager;

    @BeforeEach
    void setUp() {
        moduleDescription = Mockito.mock(ModuleDescription.class);
        moduleContext = Mockito.mock(IModuleContext.class);
        accountManager = Mockito.mock(AccountManager.class);
        contractManager = Mockito.mock(IContractManager.class);
        tradeModule = new TradeModule(moduleDescription, moduleContext, accountManager, contractManager);
    }

    @Test
    void getName() {
        String moduleName = "Test Module";
        when(moduleDescription.getModuleName()).thenReturn(moduleName);

        assertEquals(moduleName, tradeModule.getName());
    }

    @Test
    void setEnabled() {
        tradeModule.setEnabled(true);
        verify(moduleContext).setEnabled(true);

        tradeModule.setEnabled(false);
        verify(moduleContext).setEnabled(false);
    }

    @Test
    void isEnabled() {
        when(moduleContext.isEnabled()).thenReturn(true);
        assertTrue(tradeModule.isEnabled());

        when(moduleContext.isEnabled()).thenReturn(false);
        assertFalse(tradeModule.isEnabled());
    }

    @Test
    void onEvent() {
        // 在这里添加相应的测试用例，模拟不同的 NorthstarEvent 对象，并根据您的实际需求进行测试。
        // 由于您的 onEvent() 方法包含了许多条件分支，您需要为每个分支创建一个单独的测试用例。
    }

    @Test
    void getRuntimeDescription() {
        ModuleRuntimeDescription moduleRuntimeDescription = mock(ModuleRuntimeDescription.class);
        when(moduleContext.getRuntimeDescription(true)).thenReturn(moduleRuntimeDescription);
        when(moduleContext.isReady()).thenReturn(Boolean.TRUE);
        assertEquals(moduleRuntimeDescription, tradeModule.getRuntimeDescription());
    }

    @Test
    void getAccount() {
        // 添加您的测试用例，根据您的实际需求进行测试。
        // 这可能包括创建 ContractField 对象并根据预期结果来验证 getAccount() 方法的返回值。
    }

    @Test
    void getModuleDescription() {
        assertEquals(moduleDescription, tradeModule.getModuleDescription());
    }
}
