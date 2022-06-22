
package tech.quantit.northstar.gateway;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
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

    @SuppressWarnings("unchecked")
    @Test
    void testRecordsToTicks() {
        CoreField.ContractField contract = factory.makeContract("rb2210");
        BarGenerator gen = new BarGenerator(new NormalContract(contract, 0), mock(BiConsumer.class));
        long now = System.currentTimeMillis();
        long expectedTime = now - now % 60000 + 60000;
        LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(expectedTime), ZoneId.systemDefault());
        System.out.println(ldt.toLocalTime().format(DateTimeConstant.T_FORMAT_FORMATTER));
        CoreField.TickField tick1 = factory.makeTickField("rb2210", 1000);

        gen.update(tick1);

        CoreField.BarField bar = gen.finishOfBar();
        assertThat(bar.getActionTimestamp()).isEqualTo(expectedTime);
        assertThat(bar.getActionTime()).isEqualTo(ldt.toLocalTime().format(DateTimeConstant.T_FORMAT_WITH_MS_INT_FORMATTER));

        List<CoreField.BarField> records = Lists.newArrayList();
        records.add(bar);
        List<CoreField.TickField> tickFieldList = TickerGnerator.recordsToTicks(1000, records);

        assertThat(tickFieldList.size()).isEqualTo(3);
    }


}
