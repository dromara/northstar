package tech.quantit.northstar.gateway.playback.ticker;

import com.google.common.collect.Lists;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.common.constant.TickType;
import xyz.redtorch.pb.CoreField;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 生成Ticker 数据
 * 参考: https://www.fmz.com/bbs-topic/662
 *
 * v1.0版
 * @author changsong
 */
public class TickerGnerator {

    /**
     * 根据BarField生成TickField
     *
     * @param period
     * @param records
     * @return
     */
    public static List<CoreField.TickField> recordsToTicks(int period,  List<CoreField.BarField> records) {
        // http://www.metatrader5.com/en/terminal/help/tick_generation
        if (records.size() == 0) {
            return Lists.newArrayList();
        }
        List<CoreField.TickField> ticks = new ArrayList<CoreField.TickField>();
        int[] steps = {0, 2, 4, 6, 10, 12, 16, 18, 23, 25, 27, 29};

        for (var i = 0; i < records.size(); i++) {

            CoreField.BarField barField = records.get(i);
            long T = barField.getActionTimestamp();
            double O = barField.getOpenPrice();
            double H = barField.getHighPrice();
            double L = barField.getLowPrice();
            double C = barField.getClosePrice();
            double V = barField.getVolume();

            if (V > 1) {
                V = V - 1;
            }
            if ((O == H) && (L == C) && (H == L)) {
                pushTick(barField, T , O, V, ticks);
            } else if (((O == H) && (L == C)) || ((O == L) && (H == C))) {
                pushTick(barField, T, O, V, ticks);
            } else if ((O == C) && ((O == L) || (O == H))) {
                pushTick(barField, T, O, V / 2, ticks);
                pushTick(barField, T + (period / 2), (O == L ? H : L), V / 2, ticks);
            } else if ((C == H) || (C == L)) {
                pushTick(barField, T, O, V / 2, ticks);
                pushTick(barField, (long)(T + (period * 0.382)), (C == L ? H : L), V / 2, ticks);
            } else if ((O == H) || (O == L)) {
                pushTick(barField, T, O, V / 2, ticks);
                pushTick(barField, (long)(T + (period * 0.618)), (O == L ? H : L), V / 2, ticks);
            } else {
                double[] dots = {};
                var amount = V / 11;
                pushTick(barField, T, O, amount, ticks);
                if (C > O) {
                    dots = new double[]{
                            O - (O - L) * 0.75,
                            O - (O - L) * 0.5,
                            L,
                            L + (H - L) / 3.0,
                            L + (H - L) * (4 / 15.0),
                            H - (H - L) / 3.0,
                            H - (H - L) * (6 / 15.0),
                            H,
                            H - (H - C) * 0.75,
                            H - (H - C) * 0.5
                    };
                } else {
                    dots = new double[]{
                    O + (H - O) * 0.75,
                            O + (H - O) * 0.5,
                            H,
                            H - (H - L) / 3.0,
                            H - (H - L) * (4 / 15.0),
                            H - (H - L) * (2 / 3.0),
                            H - (H - L) * (9 / 15.0),
                            L,
                            L + (C - L) * 0.75,
                            L + (C - L) * 0.5
                    };
                }
                for (var j = 0; j < dots.length; j++) {
                    pushTick(barField, (long)(T + period * (steps[j + 1] / 30.0)), dots[j], amount, ticks);
                }
            }
            pushTick(barField, (long)(T + (period * 0.98)), C, 1, ticks);
        }
        return ticks;
    }

    /**
     * 放入Tick数据
     *
     * // ticks.push([Math.floor(t), Math.floor(price * pown) / pown, vol])
     */
    private static void pushTick(CoreField.BarField barField, long time, double price, double vol, List<CoreField.TickField> tickFieldList) {
        // 金额转换
        double pown = Math.pow(10, 2);
        price = Math.floor(price * pown) / pown;

        LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault());

        CoreField.TickField tickField = CoreField.TickField.newBuilder()
                    .setGatewayId(barField.getGatewayId())
                    .setUnifiedSymbol(barField.getUnifiedSymbol())
                    .setActionDay(ldt.format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
                    .setActionTime(ldt.format(DateTimeConstant.T_FORMAT_FORMATTER))

                    .setActionTimestamp(time)

                    .addAskPrice(price)
                    .addBidPrice(price)
                    .addAskVolume((int)vol)
                    .addBidVolume((int)vol)

                    .setLastPrice(price)
                    .setStatus(TickType.NORMAL_TICK.getCode())
                    .setTradingDay(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
                    .build();
        tickFieldList.add(tickField);
    }
}
