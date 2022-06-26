
package tech.quantit.northstar.gateway;

import com.alibaba.fastjson.JSON;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.json.JsonbTester;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.gateway.api.domain.BarGenerator;
import tech.quantit.northstar.gateway.api.domain.NormalContract;
import tech.quantit.northstar.gateway.playback.ticker.TickerGnerator;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreField;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 测试生产生成Ticker 数据
 *
 * @author changsong
 */
public class TickerGneratorTest {

    private TickerGnerator tickerGnerator;

    TestFieldFactory factory = new TestFieldFactory("testGateway");

    /**
     * open=close=low，high>open
     *
     * 预期 3个ticket
     */
    @SuppressWarnings("unchecked")
    @Test
    void testRecordsToTicksCase1() {
        CoreField.BarField.Builder barBuilder = CoreField.BarField.newBuilder()
                .setGatewayId("testGateway")
                .setUnifiedSymbol("rb2210");

        long now = System.currentTimeMillis();
        long expectedTime = now - now % 60000 + 60000;
        barBuilder.setActionTimestamp(expectedTime);

        barBuilder.setOpenPrice(1000);
        barBuilder.setHighPrice(1020);
        barBuilder.setClosePrice(1000);
        barBuilder.setLowPrice(1000);
        barBuilder.setVolume(20);

        CoreField.BarField barField = barBuilder.build();
        List<CoreField.BarField> records = Lists.newArrayList();
        records.add(barField);
        List<CoreField.TickField> tickFieldList = TickerGnerator.recordsToTicks(1000, records);

        assertThat(tickFieldList.size()).isEqualTo(3);
        for(CoreField.TickField tickField:tickFieldList){
            System.out.println( tickField.toString());
        }
    }


    /**
     * open=close=high，low<open
     *
     * 预期 3个ticket
     */
    @SuppressWarnings("unchecked")
    @Test
    void testRecordsToTicksCase2() {
        CoreField.BarField.Builder barBuilder = CoreField.BarField.newBuilder()
                .setGatewayId("testGateway")
                .setUnifiedSymbol("rb2210");

        long now = System.currentTimeMillis();
        long expectedTime = now - now % 60000 + 60000;
        barBuilder.setActionTimestamp(expectedTime);

        barBuilder.setOpenPrice(1000);
        barBuilder.setHighPrice(1000);
        barBuilder.setClosePrice(1000);
        barBuilder.setLowPrice(900);
        barBuilder.setVolume(20);

        CoreField.BarField barField = barBuilder.build();
        List<CoreField.BarField> records = Lists.newArrayList();
        records.add(barField);
        List<CoreField.TickField> tickFieldList = TickerGnerator.recordsToTicks(1000, records);

        assertThat(tickFieldList.size()).isEqualTo(3);
        for(CoreField.TickField tickField:tickFieldList){
            System.out.println( tickField.toString());
        }
    }

    /**
     * close>open >low , high=close
     *
     * 预期 3个ticket
     */
    @SuppressWarnings("unchecked")
    @Test
    void testRecordsToTicksCase3() {
        CoreField.BarField.Builder barBuilder = CoreField.BarField.newBuilder()
                .setGatewayId("testGateway")
                .setUnifiedSymbol("rb2210");

        long now = System.currentTimeMillis();
        long expectedTime = now - now % 60000 + 60000;
        barBuilder.setActionTimestamp(expectedTime);

        barBuilder.setOpenPrice(1000);
        barBuilder.setLowPrice(800);
        barBuilder.setHighPrice(1020);
        barBuilder.setClosePrice(1020);
        barBuilder.setVolume(20);

        CoreField.BarField barField = barBuilder.build();
        List<CoreField.BarField> records = Lists.newArrayList();
        records.add(barField);
        List<CoreField.TickField> tickFieldList = TickerGnerator.recordsToTicks(1000, records);

        assertThat(tickFieldList.size()).isEqualTo(3);
        for(CoreField.TickField tickField:tickFieldList){
            System.out.println( tickField.toString());
        }
    }

    /**
     * high >open >close , low=close
     *
     * 预期 3个ticket
     */
    @SuppressWarnings("unchecked")
    @Test
    void testRecordsToTicksCase4() {
        CoreField.BarField.Builder barBuilder = CoreField.BarField.newBuilder()
                .setGatewayId("testGateway")
                .setUnifiedSymbol("rb2210");

        long now = System.currentTimeMillis();
        long expectedTime = now - now % 60000 + 60000;
        barBuilder.setActionTimestamp(expectedTime);

        barBuilder.setOpenPrice(1000);
        barBuilder.setLowPrice(800);
        barBuilder.setHighPrice(1020);
        barBuilder.setClosePrice(800);
        barBuilder.setVolume(20);

        CoreField.BarField barField = barBuilder.build();
        List<CoreField.BarField> records = Lists.newArrayList();
        records.add(barField);
        List<CoreField.TickField> tickFieldList = TickerGnerator.recordsToTicks(1000, records);

        assertThat(tickFieldList.size()).isEqualTo(3);
        for(CoreField.TickField tickField:tickFieldList){
            System.out.println( tickField.toString());
        }
    }

    /**
     * high >close >open , low=open
     *
     * 预期 3个ticket
     */
    @SuppressWarnings("unchecked")
    @Test
    void testRecordsToTicksCase5() {
        CoreField.BarField.Builder barBuilder = CoreField.BarField.newBuilder()
                .setGatewayId("testGateway")
                .setUnifiedSymbol("rb2210");

        long now = System.currentTimeMillis();
        long expectedTime = now - now % 60000 + 60000;
        barBuilder.setActionTimestamp(expectedTime);

        barBuilder.setOpenPrice(800);
        barBuilder.setLowPrice(800);
        barBuilder.setHighPrice(1020);
        barBuilder.setClosePrice(1000);
        barBuilder.setVolume(20);

        CoreField.BarField barField = barBuilder.build();
        List<CoreField.BarField> records = Lists.newArrayList();
        records.add(barField);
        List<CoreField.TickField> tickFieldList = TickerGnerator.recordsToTicks(1000, records);

        assertThat(tickFieldList.size()).isEqualTo(3);
        for(CoreField.TickField tickField:tickFieldList){
            System.out.println( tickField.toString());
        }
    }

    /**
     * open >close >low , high=open
     *
     * 预期 3个ticket
     */
    @SuppressWarnings("unchecked")
    @Test
    void testRecordsToTicksCase6() {
        CoreField.BarField.Builder barBuilder = CoreField.BarField.newBuilder()
                .setGatewayId("testGateway")
                .setUnifiedSymbol("rb2210");

        long now = System.currentTimeMillis();
        long expectedTime = now - now % 60000 + 60000;
        barBuilder.setActionTimestamp(expectedTime);

        barBuilder.setOpenPrice(1020);
        barBuilder.setLowPrice(800);
        barBuilder.setHighPrice(1020);
        barBuilder.setClosePrice(1000);
        barBuilder.setVolume(20);

        CoreField.BarField barField = barBuilder.build();
        List<CoreField.BarField> records = Lists.newArrayList();
        records.add(barField);
        List<CoreField.TickField> tickFieldList = TickerGnerator.recordsToTicks(1000, records);

        assertThat(tickFieldList.size()).isEqualTo(3);
        for(CoreField.TickField tickField:tickFieldList){
            System.out.println( tickField.toString());
        }
    }


    /**
     * close > open, low = open, close = high
     *
     * 预期 2 个ticket
     */
    @SuppressWarnings("unchecked")
    @Test
    void testRecordsToTicksCase7() {
        CoreField.BarField.Builder barBuilder = CoreField.BarField.newBuilder()
                .setGatewayId("testGateway")
                .setUnifiedSymbol("rb2210");

        long now = System.currentTimeMillis();
        long expectedTime = now - now % 60000 + 60000;
        barBuilder.setActionTimestamp(expectedTime);

        barBuilder.setOpenPrice(800);
        barBuilder.setLowPrice(800);
        barBuilder.setHighPrice(1020);
        barBuilder.setClosePrice(1020);
        barBuilder.setVolume(20);

        CoreField.BarField barField = barBuilder.build();
        List<CoreField.BarField> records = Lists.newArrayList();
        records.add(barField);
        List<CoreField.TickField> tickFieldList = TickerGnerator.recordsToTicks(1000, records);
        for(CoreField.TickField tickField:tickFieldList){
            System.out.println( tickField.toString());
        }
        assertThat(tickFieldList.size()).isEqualTo(2);

    }


    /**
     * open >close >low , high=open
     *
     * 预期 3个ticket
     */
    @SuppressWarnings("unchecked")
    @Test
    void testRecordsToTicksCase8() {
        CoreField.BarField.Builder barBuilder = CoreField.BarField.newBuilder()
                .setGatewayId("testGateway")
                .setUnifiedSymbol("rb2210");

        long now = System.currentTimeMillis();
        long expectedTime = now - now % 60000 + 60000;
        barBuilder.setActionTimestamp(expectedTime);

        barBuilder.setOpenPrice(1020);
        barBuilder.setLowPrice(800);
        barBuilder.setHighPrice(1020);
        barBuilder.setClosePrice(800);
        barBuilder.setVolume(20);

        CoreField.BarField barField = barBuilder.build();
        List<CoreField.BarField> records = Lists.newArrayList();
        records.add(barField);
        List<CoreField.TickField> tickFieldList = TickerGnerator.recordsToTicks(1000, records);

        assertThat(tickFieldList.size()).isEqualTo(2);
        for(CoreField.TickField tickField:tickFieldList){
            System.out.println( tickField.toString());
        }
    }

    /**
     * high > close > open > low, 12个ticket
     *
     * 预期 12 个ticket
     */
    @SuppressWarnings("unchecked")
    @Test
    void testRecordsToTicksCase9() {
        CoreField.BarField.Builder barBuilder = CoreField.BarField.newBuilder()
                .setGatewayId("testGateway")
                .setUnifiedSymbol("rb2210");

        long now = System.currentTimeMillis();
        long expectedTime = now - now % 60000 + 60000;
        barBuilder.setActionTimestamp(expectedTime);

        barBuilder.setOpenPrice(900);
        barBuilder.setLowPrice(800);
        barBuilder.setHighPrice(1020);
        barBuilder.setClosePrice(1000);
        barBuilder.setVolume(24);

        CoreField.BarField barField = barBuilder.build();
        List<CoreField.BarField> records = Lists.newArrayList();
        records.add(barField);
        List<CoreField.TickField> tickFieldList = TickerGnerator.recordsToTicks(1000, records);

        assertThat(tickFieldList.size()).isEqualTo(12);
        for(CoreField.TickField tickField:tickFieldList){
            System.out.println( tickField.toString());
        }
    }


    /**
     * close = open, Doji Canlesticks
     *
     * 预期 12 个ticket
     */
    @SuppressWarnings("unchecked")
    @Test
    void testRecordsToTicksCase10() {
        CoreField.BarField.Builder barBuilder = CoreField.BarField.newBuilder()
                .setGatewayId("testGateway")
                .setUnifiedSymbol("rb2210");

        long now = System.currentTimeMillis();
        long expectedTime = now - now % 60000 + 60000;
        barBuilder.setActionTimestamp(expectedTime);

        barBuilder.setOpenPrice(1020);
//        barBuilder.setLowPrice(800);
//        barBuilder.setHighPrice(1020);
        barBuilder.setClosePrice(1020);
        barBuilder.setVolume(20);

        CoreField.BarField barField = barBuilder.build();
        List<CoreField.BarField> records = Lists.newArrayList();
        records.add(barField);
        List<CoreField.TickField> tickFieldList = TickerGnerator.recordsToTicks(1000, records);

        assertThat(tickFieldList.size()).isEqualTo(12);
        for(CoreField.TickField tickField:tickFieldList){
            System.out.println( tickField.toString());
        }
    }
}
