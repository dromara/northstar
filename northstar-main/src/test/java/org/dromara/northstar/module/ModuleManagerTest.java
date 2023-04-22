package org.dromara.northstar.module;

import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.strategy.IModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModuleManagerTest {

    private ModuleManager moduleManager;
    private IModule module1;
    private IModule module2;
    private Identifier id1;
    private Identifier id2;

    @BeforeEach
    void setUp() {
        moduleManager = new ModuleManager();
        id1 = Identifier.of("Module1");
        id2 = Identifier.of("Module2");
        module1 = mock(TradeModule.class);
        module2 = mock(TradeModule.class);
        
        when(module1.getName()).thenReturn(id1.value());
        when(module2.getName()).thenReturn(id2.value());
    }

    @Test
    void add() {
        moduleManager.add(module1);
        assertTrue(moduleManager.contains(id1));

        moduleManager.add(module2);
        assertTrue(moduleManager.contains(id2));
    }

    @Test
    void remove() {
        moduleManager.add(module1);
        moduleManager.add(module2);

        moduleManager.remove(id1);
        assertFalse(moduleManager.contains(id1));

        moduleManager.remove(id2);
        assertFalse(moduleManager.contains(id2));
    }

    @Test
    void get() {
        moduleManager.add(module1);
        moduleManager.add(module2);

        assertEquals(module1, moduleManager.get(id1));
        assertEquals(module2, moduleManager.get(id2));
    }

    @Test
    void contains() {
        assertFalse(moduleManager.contains(id1));
        assertFalse(moduleManager.contains(id2));

        moduleManager.add(module1);
        assertTrue(moduleManager.contains(id1));
        assertFalse(moduleManager.contains(id2));

        moduleManager.add(module2);
        assertTrue(moduleManager.contains(id1));
        assertTrue(moduleManager.contains(id2));
    }

    @Test
    void allModules() {
        moduleManager.add(module1);
        moduleManager.add(module2);

        List<IModule> modules = moduleManager.allModules();

        assertEquals(2, modules.size());
        assertTrue(modules.contains(module1));
        assertTrue(modules.contains(module2));
    }
}
