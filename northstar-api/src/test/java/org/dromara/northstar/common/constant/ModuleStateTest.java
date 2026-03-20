package org.dromara.northstar.common.constant;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ModuleStateTest {

	@Test
    void testIsHolding() {
        assertTrue(ModuleState.HOLDING_LONG.isHolding());
        assertTrue(ModuleState.HOLDING_SHORT.isHolding());
        assertTrue(ModuleState.HOLDING_HEDGE.isHolding());
        assertFalse(ModuleState.EMPTY.isHolding());
        assertFalse(ModuleState.EMPTY_HEDGE.isHolding());
        assertFalse(ModuleState.PLACING_ORDER.isHolding());
        assertFalse(ModuleState.PENDING_ORDER.isHolding());
        assertFalse(ModuleState.RETRIEVING_FOR_CANCEL.isHolding());
    }

    @Test
    void testIsWaiting() {
        assertTrue(ModuleState.PENDING_ORDER.isWaiting());
        assertTrue(ModuleState.RETRIEVING_FOR_CANCEL.isWaiting());
        assertFalse(ModuleState.HOLDING_LONG.isWaiting());
        assertFalse(ModuleState.HOLDING_SHORT.isWaiting());
        assertFalse(ModuleState.HOLDING_HEDGE.isWaiting());
        assertFalse(ModuleState.EMPTY.isWaiting());
        assertFalse(ModuleState.EMPTY_HEDGE.isWaiting());
        assertFalse(ModuleState.PLACING_ORDER.isWaiting());
    }

    @Test
    void testIsOrdering() {
        assertTrue(ModuleState.PLACING_ORDER.isOrdering());
        assertTrue(ModuleState.PENDING_ORDER.isOrdering());
        assertTrue(ModuleState.RETRIEVING_FOR_CANCEL.isOrdering());
        assertFalse(ModuleState.HOLDING_LONG.isOrdering());
        assertFalse(ModuleState.HOLDING_SHORT.isOrdering());
        assertFalse(ModuleState.HOLDING_HEDGE.isOrdering());
        assertFalse(ModuleState.EMPTY.isOrdering());
        assertFalse(ModuleState.EMPTY_HEDGE.isOrdering());
    }

    @Test
    void testIsEmpty() {
        assertTrue(ModuleState.EMPTY.isEmpty());
        assertTrue(ModuleState.EMPTY_HEDGE.isEmpty());
        assertFalse(ModuleState.HOLDING_LONG.isEmpty());
        assertFalse(ModuleState.HOLDING_SHORT.isEmpty());
        assertFalse(ModuleState.HOLDING_HEDGE.isEmpty());
        assertFalse(ModuleState.PLACING_ORDER.isEmpty());
        assertFalse(ModuleState.PENDING_ORDER.isEmpty());
        assertFalse(ModuleState.RETRIEVING_FOR_CANCEL.isEmpty());
    }

}
