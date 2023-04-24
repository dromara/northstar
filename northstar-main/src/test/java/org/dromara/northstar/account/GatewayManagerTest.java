package org.dromara.northstar.account;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.dromara.northstar.common.constant.GatewayUsage;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.gateway.Gateway;
import org.dromara.northstar.gateway.MarketGateway;
import org.dromara.northstar.gateway.TradeGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GatewayManagerTest {

    GatewayManager gatewayManager;
    
    TradeGateway gateway = mock(TradeGateway.class);
    MarketGateway marketGateway = mock(MarketGateway.class);

    @BeforeEach
    void setUp() {
        gatewayManager = new GatewayManager();
        when(gateway.gatewayId()).thenReturn("tdGateway");
        when(gateway.gatewayDescription()).thenReturn(GatewayDescription.builder().gatewayUsage(GatewayUsage.TRADE).build());
        when(marketGateway.gatewayId()).thenReturn("mktGateway");
        when(marketGateway.gatewayDescription()).thenReturn(GatewayDescription.builder().gatewayUsage(GatewayUsage.MARKET_DATA).build());
    }

    @Test
    void testAddAndRemoveGateway() {
        Identifier id = Identifier.of(gateway.gatewayId());

        assertFalse(gatewayManager.contains(id));

        gatewayManager.add(gateway);
        assertTrue(gatewayManager.contains(id));

        gatewayManager.remove(id);
        assertFalse(gatewayManager.contains(id));
    }

    @Test
    void testGetGateway() {
        Identifier id = Identifier.of(gateway.gatewayId());

        gatewayManager.add(gateway);
        Gateway retrievedGateway = gatewayManager.get(id);

        assertEquals(gateway, retrievedGateway);
    }

    @Test
    void testTradeAndMarketGateways() {
        gatewayManager.add(gateway);
        gatewayManager.add(marketGateway);

        List<TradeGateway> tradeGateways = gatewayManager.tradeGateways();
        List<MarketGateway> marketGateways = gatewayManager.marketGateways();

        assertEquals(1, tradeGateways.size());
        assertEquals(gateway, tradeGateways.get(0));

        assertEquals(1, marketGateways.size());
        assertEquals(marketGateway, marketGateways.get(0));
    }

    @Test
    void testAllGateways() {
        gatewayManager.add(gateway);
        gatewayManager.add(marketGateway);

        List<Gateway> allGateways = gatewayManager.allGateways();

        assertEquals(2, allGateways.size());
        assertTrue(allGateways.contains(gateway));
        assertTrue(allGateways.contains(marketGateway));
    }
}
