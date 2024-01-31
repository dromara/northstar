package org.dromara.northstar.common.constant;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ChannelTypeTest {

	@Test
    public void testPlaybackUsage() {
        assertArrayEquals(new GatewayUsage[] {GatewayUsage.MARKET_DATA}, ChannelType.PLAYBACK.usage());
    }

    @Test
    public void testPlaybackAllowDuplication() {
        assertTrue(ChannelType.PLAYBACK.allowDuplication());
    }

    @Test
    public void testSimUsage() {
        assertArrayEquals(new GatewayUsage[] {GatewayUsage.MARKET_DATA, GatewayUsage.TRADE}, ChannelType.SIM.usage());
    }

    @Test
    public void testCtpUsage() {
        assertArrayEquals(new GatewayUsage[] {GatewayUsage.MARKET_DATA, GatewayUsage.TRADE}, ChannelType.CTP.usage());
    }

    @Test
    public void testTigerUsage() {
        assertArrayEquals(new GatewayUsage[] {GatewayUsage.MARKET_DATA, GatewayUsage.TRADE}, ChannelType.TIGER.usage());
    }

    @Test
    public void testCtpSimUsage() {
        assertArrayEquals(new GatewayUsage[] {GatewayUsage.MARKET_DATA, GatewayUsage.TRADE}, ChannelType.CTP_SIM.usage());
    }

    @Test
    public void testCtpSimAdminOnly() {
        assertTrue(ChannelType.CTP_SIM.adminOnly());
    }

    @Test
    public void testOkxUsage() {
        assertArrayEquals(new GatewayUsage[] {GatewayUsage.MARKET_DATA, GatewayUsage.TRADE}, ChannelType.OKX.usage());
    }

    @Test
    public void testBianUsage() {
        assertArrayEquals(new GatewayUsage[] {GatewayUsage.MARKET_DATA, GatewayUsage.TRADE}, ChannelType.BIAN.usage());
    }

    @Test
    public void testDefaultAdminOnly() {
        // Test the default adminOnly() method for an enum constant that does not override it
        assertFalse(ChannelType.SIM.adminOnly());
        assertFalse(ChannelType.CTP.adminOnly());
        // ... Test for all other enum constants that do not override adminOnly
    }

    @Test
    public void testDefaultAllowDuplication() {
        // Test the default allowDuplication() method for enum constants that do not override it
        assertFalse(ChannelType.SIM.allowDuplication());
        assertFalse(ChannelType.CTP.allowDuplication());
        // ... Test for all other enum constants that do not override allowDuplication
    }

}
