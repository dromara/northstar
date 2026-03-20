package org.dromara.northstar.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.dromara.northstar.common.constant.ChannelType;
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
    
    GatewayDescription gd1 = GatewayDescription.builder().gatewayId("tdGateway").gatewayUsage(GatewayUsage.MARKET_DATA).channelType(ChannelType.SIM).build();
    GatewayDescription gd2 = GatewayDescription.builder().gatewayId("mktGateway").gatewayUsage(GatewayUsage.TRADE).channelType(ChannelType.SIM).build();

    @BeforeEach
    void setUp() {
        gatewayManager = new GatewayManager();
        when(gateway.getDescription()).thenReturn(gd2);
        when(marketGateway.getDescription()).thenReturn(gd1);
    }

    @Test
    void testAddAndRemoveGateway() {
        Identifier id = Identifier.of(gateway.getDescription().getGatewayId());

        assertFalse(gatewayManager.contains(id));

        gatewayManager.add(id, gateway);
        assertTrue(gatewayManager.contains(id));

        gatewayManager.remove(id);
        assertFalse(gatewayManager.contains(id));
    }

    @Test
    void testGetGateway() {
        Identifier id = Identifier.of(gateway.getDescription().getGatewayId());

        gatewayManager.add(id, gateway);
        Gateway retrievedGateway = gatewayManager.get(id);

        assertEquals(gateway, retrievedGateway);
    }

    @Test
    void testTradeAndMarketGateways() {
        gatewayManager.add(Identifier.of(gd2.getGatewayId()), gateway);
        gatewayManager.add(Identifier.of(gd1.getGatewayId()), marketGateway);

        List<TradeGateway> tradeGateways = gatewayManager.tradeGateways();
        List<MarketGateway> marketGateways = gatewayManager.marketGateways();

        assertEquals(1, tradeGateways.size());
        assertEquals(gateway, tradeGateways.get(0));

        assertEquals(1, marketGateways.size());
        assertEquals(marketGateway, marketGateways.get(0));
    }

    @Test
    void testAllGateways() {
        gatewayManager.add(Identifier.of(gd2.getGatewayId()), gateway);
        gatewayManager.add(Identifier.of(gd1.getGatewayId()), marketGateway);

        List<Gateway> allGateways = gatewayManager.findAll();

        assertEquals(2, allGateways.size());
        assertTrue(allGateways.contains(gateway));
        assertTrue(allGateways.contains(marketGateway));
    }
}
