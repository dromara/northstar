package org.dromara.northstar.module;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.dromara.northstar.common.model.ModuleDescription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

class TradeModuleTest {
    private TradeModule tradeModule;
    private ModuleDescription moduleDescription;
    private ModuleContext moduleContext;

    @BeforeEach
    void setUp() {
        moduleDescription = Mockito.mock(ModuleDescription.class);
        moduleContext = Mockito.mock(ModuleContext.class);
        when(moduleContext.getModuleDescription()).thenReturn(moduleDescription);
        when(moduleContext.getLogger(any())).thenReturn(mock(Logger.class));
        tradeModule = new TradeModule(moduleContext);
    }


    @Test
    void setEnabled() {
        tradeModule.setEnabled(true);
        verify(moduleContext).setEnabled(true);

        tradeModule.setEnabled(false);
        verify(moduleContext).setEnabled(false);
    }

    @Test
    void onEvent() {
        // 在这里添加相应的测试用例，模拟不同的 NorthstarEvent 对象，并根据您的实际需求进行测试。
        // 由于您的 onEvent() 方法包含了许多条件分支，您需要为每个分支创建一个单独的测试用例。
    }

    @Test
    void getAccount() {
    }

}
