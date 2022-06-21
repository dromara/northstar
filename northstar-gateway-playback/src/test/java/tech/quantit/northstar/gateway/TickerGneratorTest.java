
package tech.quantit.northstar.gateway;

import tech.quantit.northstar.gateway.playback.ticker.TickerGnerator;
import test.common.TestFieldFactory;

/**
 * 测试生产生成Ticker 数据
 *
 * @author changsong
 */
public class TickerGneratorTest {

    private TickerGnerator tickerGnerator;

    private TestFieldFactory factory = new TestFieldFactory("test");
    //
    // @BeforeEach
    // void prepare() {
    //     FastEventEngine feEngine = mock(FastEventEngine.class);
    //     CoreField.GatewaySettingField settings = CoreField.GatewaySettingField.newBuilder()
    //             .setGatewayId("gatewayId")
    //             .build();
    //     SimAccount simAccount = mock(SimAccount.class);
    //     gateway = new SimTradeGatewayLocal(feEngine, mock(SimMarket.class), settings, "bindedGatewayId", simAccount, mock(GlobalMarketRegistry.class));
    // }
    //
    // @Test
    // void testConnectAndDisConnect() {
    //     gateway.connect();
    //     verify(gateway.feEngine).emitEvent(eq(NorthstarEventType.CONNECTED), anyString());
    //
    //     gateway.disconnect();
    //     verify(gateway.feEngine).emitEvent(eq(NorthstarEventType.DISCONNECTED), anyString());
    // }

}
