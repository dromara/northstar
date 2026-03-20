package org.dromara.northstar.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.strategy.IModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    }

    @Test
    void add() {
        moduleManager.add(id1, module1);
        assertTrue(moduleManager.contains(id1));

        moduleManager.add(id2, module2);
        assertTrue(moduleManager.contains(id2));
    }

    @Test
    void remove() {
        moduleManager.add(id1, module1);
        moduleManager.add(id2, module2);

        moduleManager.remove(id1);
        assertFalse(moduleManager.contains(id1));

        moduleManager.remove(id2);
        assertFalse(moduleManager.contains(id2));
    }

    @Test
    void get() {
        moduleManager.add(id1, module1);
        moduleManager.add(id2, module2);

        assertEquals(module1, moduleManager.get(id1));
        assertEquals(module2, moduleManager.get(id2));
    }

    @Test
    void contains() {
        assertFalse(moduleManager.contains(id1));
        assertFalse(moduleManager.contains(id2));

        moduleManager.add(id1, module1);
        assertTrue(moduleManager.contains(id1));
        assertFalse(moduleManager.contains(id2));

        moduleManager.add(id2, module2);
        assertTrue(moduleManager.contains(id1));
        assertTrue(moduleManager.contains(id2));
    }

    @Test
    void allModules() {
        moduleManager.add(id1, module1);
        moduleManager.add(id2, module2);

        List<IModule> modules = moduleManager.findAll();

        assertEquals(2, modules.size());
        assertTrue(modules.contains(module1));
        assertTrue(modules.contains(module2));
    }
}
