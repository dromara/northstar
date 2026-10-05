package org.dromara.northstar.rl.env;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.dromara.northstar.common.model.core.Bar;
import org.junit.jupiter.api.Test;

class MarketReplayTest {

    private static List<Bar> bars(int n) {
        List<Bar> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int idx = i;
            result.add(Bar.builder()
                    .actionDay(LocalDate.of(2024, 1, 1).plusDays(i))
                    .closePrice(1000 + idx)
                    .build());
        }
        return result;
    }

    @Test
    void warmupPrefixIsSkippedThenOkThenExhausted() {
        MarketReplay replay = new MarketReplay(bars(100));
        replay.locate(50, 20, 10);

        int warmup = 0, ok = 0;
        AdvanceResult r;
        while ((r = replay.advanceBar()) != AdvanceResult.EXHAUSTED) {
            if (r == AdvanceResult.WARMUP) {
                warmup++;
            } else {
                ok++;
            }
        }
        assertEquals(10, warmup, "预热段只喂数据");
        assertEquals(20, ok, "正式段长度");
        assertEquals(0, replay.barsRemaining());
    }

    @Test
    void warmupPrefixClampedWhenHistoryInsufficient() {
        MarketReplay replay = new MarketReplay(bars(30));
        replay.locate(5, 10, 20);	// 历史前缀只有 5 根，预热需求 20 根 -> clamp
        int warmup = 0;
        while (replay.advanceBar() != AdvanceResult.EXHAUSTED) {
            if (replay.cursor() < 5) {
                warmup++;
            }
        }
        assertEquals(5, warmup);
    }

    @Test
    void episodeTruncatedAtDataEnd() {
        MarketReplay replay = new MarketReplay(bars(15));
        replay.locate(10, 100, 5);
        int ok = 0;
        while (replay.advanceBar() != AdvanceResult.EXHAUSTED) {
            if (replay.cursor() >= 10) {
                ok++;
            }
        }
        assertEquals(5, ok, "超出数据末尾时截断");
    }

    @Test
    void rejectsInvalidSlice() {
        MarketReplay replay = new MarketReplay(bars(10));
        assertThrows(IllegalArgumentException.class, () -> replay.locate(-1, 5, 0));
        assertThrows(IllegalArgumentException.class, () -> replay.locate(10, 5, 0));
        assertThrows(IllegalArgumentException.class, () -> replay.locate(3, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new MarketReplay(List.of()));
    }

    @Test
    void lowerBoundFindsFirstBarOnOrAfterDate() {
        MarketReplay replay = new MarketReplay(bars(10));
        assertEquals(0, replay.lowerBound(LocalDate.of(2023, 12, 31)));
        assertEquals(5, replay.lowerBound(LocalDate.of(2024, 1, 6)));
        assertEquals(10, replay.lowerBound(LocalDate.of(2024, 2, 1)));
    }
}
