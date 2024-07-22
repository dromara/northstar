package org.dromara.northstar.gateway.tiger.util;

import com.tigerbrokers.stock.openapi.client.TigerApiException;
import com.tigerbrokers.stock.openapi.client.config.ClientConfig;
import com.tigerbrokers.stock.openapi.client.https.domain.future.item.FutureKlineBatchItem;
import com.tigerbrokers.stock.openapi.client.https.domain.future.item.FutureKlineItem;
import com.tigerbrokers.stock.openapi.client.https.domain.future.model.FutureKlineModel;
import com.tigerbrokers.stock.openapi.client.https.domain.quote.item.KlineItem;
import com.tigerbrokers.stock.openapi.client.https.domain.quote.item.KlinePoint;
import com.tigerbrokers.stock.openapi.client.https.domain.quote.model.QuoteKlineModel;
import com.tigerbrokers.stock.openapi.client.https.request.future.FutureKlineRequest;
import com.tigerbrokers.stock.openapi.client.https.request.quote.QuoteKlineRequest;
import com.tigerbrokers.stock.openapi.client.https.response.future.FutureKlineResponse;
import com.tigerbrokers.stock.openapi.client.https.response.quote.QuoteKlineResponse;
import com.tigerbrokers.stock.openapi.client.struct.enums.*;
import com.tigerbrokers.stock.openapi.client.util.DateUtils;
import org.dromara.northstar.gateway.tiger.TigerHttpClient;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PageTokenUtil {
    public static int DEFAULT_PAGE_SIZE = 1000;
    public static int DEFAULT_BATCH_TIME = 10;
    public static int MAX_TOTAL_SIZE = 10000;
    public static long DEFAULT_TIME_INTERVAL = 2000L;
    public static final KlinePoint RETURN_KLINE = new KlinePoint();
    public static final FutureKlineItem RETURN_FUTURE_KLINE = new FutureKlineItem();
    public static TigerHttpClient client;

    private PageTokenUtil() {
    }

    /**
     * get kline data by page, return the merge result(use default pageSize:1000, 10 queries in total)
     * @param symbol symbol
     * @param period kline tpye
     * @param beginTime begin time. format:"2022-04-25 00:00:00"
     * @param endTime end time. formae:"2022-04-28 00:00:00"
     * @throws TigerApiException
     */
    public static List<KlinePoint> getKlineByPage(TigerHttpClient tigerHttpClient, String symbol, KType period,
                                                  String beginTime, String endTime) throws TigerApiException {
        client = tigerHttpClient;
        return getKlineByPage(symbol, period, beginTime, endTime,
                ClientConfig.DEFAULT_CONFIG.getDefaultTimeZone(),
                RightOption.br, DEFAULT_PAGE_SIZE, DEFAULT_PAGE_SIZE * DEFAULT_BATCH_TIME, DEFAULT_TIME_INTERVAL);
    }

    /**
     * get kline data by page, return the merge result(use default pageSize:1000, 10 queries in total)
     * @param symbol symbol
     * @param period kline tpye
     * @param beginTime begin time. format:"2022-04-25 00:00:00"
     * @param endTime end time. formae:"2022-04-28 00:00:00"
     * @param totalSize total size. the max value is 10000
     * @throws TigerApiException
     */
    public static List<KlinePoint> getKlineByPage(String symbol, KType period,
                                                  String beginTime, String endTime, int totalSize) throws TigerApiException {
        return getKlineByPage(symbol, period, beginTime, endTime,
                ClientConfig.DEFAULT_CONFIG.getDefaultTimeZone(),
                RightOption.br, DEFAULT_PAGE_SIZE, totalSize, DEFAULT_TIME_INTERVAL);
    }

    /**
     * get kline data by page, return the merge result
     * @param symbol symbol
     * @param period kline tpye
     * @param beginTime begin time. format:"2022-04-25 00:00:00"
     * @param endTime end time. formae:"2022-04-28 00:00:00"
     * @param zoneId zone, the default is NewYork
     * @param right adjust type. the default is br(adjusted)
     * @param pageSize page size. the default is 1000
     * @param totalSize total size. the default is 10000
     * @param timeInterval request interval milliseconds, the default is 2000
     * @throws TigerApiException
     */
    public static List<KlinePoint> getKlineByPage(String symbol, KType period,
                                                  String beginTime, String endTime, TimeZoneId zoneId,
                                                  RightOption right, int pageSize, int totalSize, long timeInterval) throws TigerApiException {
        return getKlineByPage(RETURN_KLINE, symbol, period == null ? null : period.name(),
                beginTime, endTime, zoneId, right, pageSize, totalSize, timeInterval);
    }

    /**
     * get futrue kline data by page, return the merge result(use default pageSize:1000, 10 queries in total)
     * @param symbol contract code
     * @param period kline tpye
     * @param beginTime begin time. format:"2022-04-25 00:00:00"
     * @param endTime end time. formae:"2022-04-28 00:00:00"
     * @throws TigerApiException
     */
    public static List<FutureKlineItem> getKlineByPage(String symbol, FutureKType period,
                                                       String beginTime, String endTime) throws TigerApiException {
        return getKlineByPage(symbol, period, beginTime, endTime,
                ClientConfig.DEFAULT_CONFIG.getDefaultTimeZone(),
                DEFAULT_PAGE_SIZE, DEFAULT_PAGE_SIZE * DEFAULT_BATCH_TIME, DEFAULT_TIME_INTERVAL);
    }

    /**
     * get futrue kline data by page, return the merge result(use default pageSize:1000, 10 queries in total)
     * @param symbol contract code
     * @param period kline tpye
     * @param beginTime begin time. format:"2022-04-25 00:00:00"
     * @param endTime end time. formae:"2022-04-28 00:00:00"
     * @param totalSize total size. the max value is 10000
     * @throws TigerApiException
     */
    public static List<FutureKlineItem> getKlineByPage(String symbol, FutureKType period,
                                                       String beginTime, String endTime, int totalSize) throws TigerApiException {
        return getKlineByPage(symbol, period, beginTime, endTime,
                ClientConfig.DEFAULT_CONFIG.getDefaultTimeZone(),
                DEFAULT_PAGE_SIZE, totalSize, DEFAULT_TIME_INTERVAL);
    }

    /**
     * get futrue kline data by page, return the merge result
     * @param symbol contract code
     * @param period kline tpye
     * @param beginTime begin time. format:"2022-04-25 00:00:00"
     * @param endTime end time. formae:"2022-04-28 00:00:00"
     * @param zoneId zone, the default is NewYork
     * @param pageSize page size. the default is 1000
     * @param totalSize total size. the default is 10000
     * @param timeInterval request interval milliseconds, the default is 2000
     * @throws TigerApiException
     */
    public static List<FutureKlineItem> getKlineByPage(String symbol, FutureKType period,
                                                       String beginTime, String endTime, TimeZoneId zoneId, int pageSize, int totalSize, long timeInterval) throws TigerApiException {
        return getKlineByPage(RETURN_FUTURE_KLINE, symbol, period == null ? null : period.name(),
                beginTime, endTime, zoneId, null, pageSize, totalSize, timeInterval);
    }

    /**
     * get kline data by page, return the merge result
     * @param t return type,only for FutureKlineItem(MethodName.FUTURE_KLINE) and KlineItem(MethodName.KLINE)
     * @param symbol symbol or future contract code
     * @param period kline tpye
     * @param beginTime begin time. format:"2022-04-25 00:00:00"
     * @param endTime end time. formae:"2022-04-28 00:00:00"
     * @param zoneId zone, the default is NewYork
     * @param right adjust type（only for stock）. the default is br(adjusted)
     * @param pageSize page size. the default is 1000
     * @param totalSize total size. the default is 10000
     * @param timeInterval request interval milliseconds, the default is 2000
     * @throws TigerApiException
     */
    public static <T> List<T> getKlineByPage(T t, String symbol, String period,
                                             String beginTime, String endTime, TimeZoneId zoneId,
                                             RightOption right, int pageSize, int totalSize, long timeInterval) throws TigerApiException {

        if (t == null || !(t instanceof KlinePoint || t instanceof FutureKlineItem)) {
            throw new TigerApiException(TigerApiCode.HTTP_BIZ_PARAM_VALUE_ERROR, "return type");
        }
        boolean isKline = (t instanceof KlinePoint);
        if (symbol == null) {
            throw new TigerApiException(TigerApiCode.HTTP_BIZ_PARAM_EMPTY_ERROR, "symbol");
        }
        if (totalSize <= 0) {
            totalSize = DEFAULT_PAGE_SIZE * DEFAULT_BATCH_TIME;
        }
        if (totalSize > MAX_TOTAL_SIZE) {
            throw new TigerApiException(TigerApiCode.HTTP_BIZ_PARAM_ERROR, "'totalSize' exceeds maximum: " + MAX_TOTAL_SIZE);
        }
        if (pageSize <= 0) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        if (totalSize < pageSize) {
            pageSize = totalSize;
        }
        if (zoneId == null) {
            zoneId = ClientConfig.DEFAULT_CONFIG.getDefaultTimeZone();
        }
        if (timeInterval < 1000) {
            timeInterval = DEFAULT_TIME_INTERVAL;
        }

        List<T> results = new ArrayList<>(totalSize);
        List<String> symbols = new ArrayList<>();
        symbols.add(symbol);
        if (isKline) {
            KType kType = KType.day;
            if (period != null && KType.valueOf(period) != null) {
                kType = KType.valueOf(period);
            }
            QuoteKlineRequest request = QuoteKlineRequest.newRequest(symbols, kType,
                    beginTime, endTime, zoneId);
            request.withLimit(pageSize);
            request.withRight(right == null ? RightOption.br : right);

            do {
                if (client == null) {
                    client = TigerHttpClient.getInstance();
                }
                QuoteKlineResponse response = client.execute(request);
                if (!response.isSuccess()) {
                    throw new TigerApiException(response.getMessage());
                }
                if (response.getKlineItems().size() == 0) {
                    break;
                }
                KlineItem klineItem = response.getKlineItems().get(0);
                results.addAll((List<T>) klineItem.getItems());

                if (klineItem.getNextPageToken() == null) {
                    break;
                }
                // 60 times per minute
                try {
                    TimeUnit.MILLISECONDS.sleep(timeInterval);
                } catch (InterruptedException ignoreIE) {
                }
                // set pagination token then query the next page
                request.withPageToken(klineItem.getNextPageToken());
                if (results.size() + pageSize > totalSize) {
                    ((QuoteKlineModel) request.getApiModel()).setLimit(totalSize - results.size());
                }
            } while (results.size() < totalSize);
        } else {
            Date beginDate = DateUtils.getZoneDate(beginTime, zoneId);
            if (beginDate == null) {
                throw new TigerApiException(TigerApiCode.HTTP_BIZ_PARAM_VALUE_ERROR, "beginTime");
            }
            Date endDate = DateUtils.getZoneDate(endTime, zoneId);
            if (endDate == null) {
                throw new TigerApiException(TigerApiCode.HTTP_BIZ_PARAM_VALUE_ERROR, "endTime");
            }
            FutureKType kType = FutureKType.day;
            if (period != null && FutureKType.valueOf(period) != null) {
                kType = FutureKType.valueOf(period);
            }

            FutureKlineRequest request = FutureKlineRequest.newRequest(symbols, kType,
                    beginDate.getTime(), endDate.getTime(), pageSize);
            do {
                if (client == null) {
                    client = TigerHttpClient.getInstance();
                }
                FutureKlineResponse response = TigerHttpClient.getInstance().execute(request);
                if (!response.isSuccess()) {
                    throw new TigerApiException(response.getMessage());
                }
                if (response.getFutureKlineItems().size() == 0) {
                    break;
                }
                FutureKlineBatchItem klineItem = response.getFutureKlineItems().get(0);
                results.addAll((List<T>) klineItem.getItems());

                if (klineItem.getNextPageToken() == null) {
                    break;
                }
                // 60 times per minute
                try {
                    TimeUnit.MILLISECONDS.sleep(timeInterval);
                } catch (InterruptedException ignoreIE) {
                }
                // set pagination token then query the next page
                request.withPageToken(klineItem.getNextPageToken());
                if (results.size() + pageSize > totalSize) {
                    ((FutureKlineModel) request.getApiModel()).setLimit(totalSize - results.size());
                }
            } while (results.size() < totalSize);
        }
        return results;
    }

}
