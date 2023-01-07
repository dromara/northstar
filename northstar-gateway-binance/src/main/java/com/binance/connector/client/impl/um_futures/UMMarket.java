package com.binance.connector.client.impl.um_futures;

import com.binance.connector.client.enums.HttpMethod;
import com.binance.connector.client.utils.ParameterChecker;
import java.util.LinkedHashMap;
import com.binance.connector.client.impl.futures.Market; 

/**
 * <h2>USDⓈ-Margined Market Endpoints</h2>
 * All endpoints under the
 * <a href="https://binance-docs.github.io/apidocs/futures/en/#market-data-endpoints">Market Data Endpoint</a>
 * section of the API documentation will be implemented in this class.
 * <br>
 * Response will be returned in <i>String format</i>.
 */
public class UMMarket extends Market {
    public UMMarket(String productUrl, String baseUrl, String apiKey, boolean showLimitUsage) {
        super(productUrl, baseUrl, apiKey, showLimitUsage);
    }

    /**
     * Mark Price and Funding Rate
     * <br><br>
     * GET /v1/premiumIndex
     * <br>
     * @param
     * parameters LinkedHashedMap of String,Object pair
     *            where String is the name of the parameter and Object is the value of the parameter
     * <br><br>
     * symbol -- optional/string -- the trading symbol <br>
     * @return String
     * @see <a href="https://binance-docs.github.io/apidocs/futures/en/#mark-price">
     *     https://binance-docs.github.io/apidocs/futures/en/#mark-price</a>
     */
    public String markPrice(LinkedHashMap<String, Object> parameters) {
        return super.markPrice(parameters);
    }

    /**
     * 24 hour rolling window price change statistics. Careful when accessing this with no symbol.
     * <br><br>
     * GET /v1/ticker/24hr
     * <br>
     * @param
     * parameters LinkedHashedMap of String,Object pair
     *            where String is the name of the parameter and Object is the value of the parameter
     * <br><br>
     * symbol -- optional/string -- the trading symbol <br>
     * @return String
     * @see <a href="https://binance-docs.github.io/apidocs/futures/en/#24hr-ticker-price-change-statistics">
     *     https://binance-docs.github.io/apidocs/futures/en/#24hr-ticker-price-change-statistics</a>
     */
    public String ticker24H(LinkedHashMap<String, Object> parameters) {
        return super.ticker24H(parameters);
    }

    /**
     * Latest price for a symbol or symbols.
     * <br><br>
     * GET /v1/ticker/price
     * <br>
     * https://binance-docs.github.io/apidocs/futures/en/#symbol-price-ticker
     * @param
     * parameters LinkedHashedMap of String,Object pair
     *            where String is the name of the parameter and Object is the value of the parameter
     * <br><br>
     * symbol -- optional/string -- the trading symbol <br>
     * @return String
     * @see <a href="https://binance-docs.github.io/apidocs/futures/en/#symbol-price-ticker">
     *     https://binance-docs.github.io/apidocs/futures/en/#symbol-price-ticker</a>
     */
    public String tickerSymbol(LinkedHashMap<String, Object> parameters) {
        return super.tickerSymbol(parameters);
    }

    /**
     * Best price/qty on the order book for a symbol or symbols.
     * <br><br>
     * GET /v1/ticker/bookTicker
     * <br>
     * https://binance-docs.github.io/apidocs/futures/en/#symbol-order-book-ticker
     * @param
     * parameters LinkedHashedMap of String,Object pair
     *            where String is the name of the parameter and Object is the value of the parameter
     * <br><br>
     * symbol -- optional/string -- the trading symbol <br>
     * @return String
     * @see <a href="https://binance-docs.github.io/apidocs/futures/en/#symbol-order-book-ticker">
     *     https://binance-docs.github.io/apidocs/futures/en/#symbol-order-book-ticker</a>
     */
    public String bookTicker(LinkedHashMap<String, Object> parameters) {
        return super.bookTicker(parameters);
    }

    /**
     * Open Interest History
     * <br><br>
     * GET /futures/data/openInterestHist
     * <br>
     * https://binance-docs.github.io/apidocs/futures/en/#open-interest-statistics
     * @param
     * parameters LinkedHashedMap of String,Object pair
     *            where String is the name of the parameter and Object is the value of the parameter
     * <br><br>
     * symbol -- mandatory/string -- the trading pair <br>
     * period -- mandatory/enum -- "5m","15m","30m","1h","2h","4h","6h","12h","1d" <br>
     * limit -- optional/long -- default 30, max 500 <br>
     * startTime -- optional/long -- Start Time <br>
     * endTime -- optional/long -- End Time <br>
     * @return String
     * @see <a href="https://binance-docs.github.io/apidocs/futures/en/#open-interest-statistics">
     *     https://binance-docs.github.io/apidocs/futures/en/#open-interest-statistics</a>
     */
    public String openInterestStatistics(LinkedHashMap<String, Object> parameters) {
        ParameterChecker.checkParameter(parameters, "symbol", String.class);
        ParameterChecker.checkParameter(parameters, "period", String.class);
        return super.openInterestStatistics(parameters);
    }

    /**
     * Top Trader Long/Short Ratio (Positions)
     * <br><br>
     * GET /futures/data/topLongShortPositionRatio
     * <br>
     * https://binance-docs.github.io/apidocs/futures/en/#top-trader-long-short-ratio-positions
     * @param
     * parameters LinkedHashedMap of String,Object pair
     *            where String is the name of the parameter and Object is the value of the parameter
     * <br><br>
     * symbol -- mandatory/string -- the trading pair <br>
     * period -- mandatory/enum -- "5m","15m","30m","1h","2h","4h","6h","12h","1d" <br>
     * limit -- optional/long -- default 30, max 500 <br>
     * startTime -- optional/long -- Start Time <br>
     * endTime -- optional/long -- End Time <br>
     * @return String
     * @see <a href="https://binance-docs.github.io/apidocs/futures/en/#top-trader-long-short-ratio-positions">
     *     https://binance-docs.github.io/apidocs/futures/en/#top-trader-long-short-ratio-positions</a>
     */
    public String topTraderLongShortPos(LinkedHashMap<String, Object> parameters) {
        ParameterChecker.checkParameter(parameters, "symbol", String.class);
        ParameterChecker.checkParameter(parameters, "period", String.class);
        return super.topTraderLongShortPos(parameters);
    }

    /**
     * Top Trader Long/Short Ratio (Accounts)
     * <br><br>
     * GET /futures/data/topLongShortAccountRatio
     * <br>
     * https://binance-docs.github.io/apidocs/delivery/en/#top-trader-long-short-ratio-accounts
     * @param
     * parameters LinkedHashedMap of String,Object pair
     *            where String is the name of the parameter and Object is the value of the parameter
     * <br><br>
     * symbol -- mandatory/string -- the trading pair <br>
     * period -- mandatory/enum -- "5m","15m","30m","1h","2h","4h","6h","12h","1d" <br>
     * limit -- optional/long -- default 30, max 500 <br>
     * startTime -- optional/long -- Start Time <br>
     * endTime -- optional/long -- End Time <br>
     * @return String
     * @see <a href="https://binance-docs.github.io/apidocs/delivery/en/#top-trader-long-short-ratio-accounts">
     *     https://binance-docs.github.io/apidocs/delivery/en/#top-trader-long-short-ratio-accounts</a>
     */
    public String topTraderLongShortAccs(LinkedHashMap<String, Object> parameters) {
        ParameterChecker.checkParameter(parameters, "symbol", String.class);
        ParameterChecker.checkParameter(parameters, "period", String.class);
        return super.topTraderLongShortAccs(parameters);
    }

    /**
     * Long/Short Ratio
     * <br><br>
     * GET /futures/data/globalLongShortAccountRatio
     * <br>
     * https://binance-docs.github.io/apidocs/futures/en/#long-short-ratio
     * @param
     * parameters LinkedHashedMap of String,Object pair
     *            where String is the name of the parameter and Object is the value of the parameter
     * <br><br>
     * symbol -- mandatory/string -- the trading pair <br>
     * period -- mandatory/enum -- "5m","15m","30m","1h","2h","4h","6h","12h","1d" <br>
     * limit -- optional/long -- default 30, max 500 <br>
     * startTime -- optional/long -- Start Time <br>
     * endTime -- optional/long -- End Time <br>
     * @return String
     * @see <a href="https://binance-docs.github.io/apidocs/futures/en/#long-short-ratio">
     *     https://binance-docs.github.io/apidocs/futures/en/#long-short-ratio</a>
     */
    public String longShortRatio(LinkedHashMap<String, Object> parameters) {
        ParameterChecker.checkParameter(parameters, "symbol", String.class);
        ParameterChecker.checkParameter(parameters, "period", String.class);
        return super.longShortRatio(parameters);
    }

    private final String TAKE_BUY_SELL_VOLUME = "/futures/data/takerlongshortRatio";
    /**
     * Taker Buy/Sell Volume
     * <br><br>
     * GET /futures/data/takerlongshortRatio
     * <br>
     * https://binance-docs.github.io/apidocs/futures/en/#taker-buy-sell-volume
     * @param
     * parameters LinkedHashedMap of String,Object pair
     *            where String is the name of the parameter and Object is the value of the parameter
     * <br><br>
     * symbol -- mandatory/string -- the trading pair <br>
     * period -- mandatory/enum -- "5m","15m","30m","1h","2h","4h","6h","12h","1d" <br>
     * limit -- optional/long -- default 30, max 500 <br>
     * startTime -- optional/long -- Start Time <br>
     * endTime -- optional/long -- End Time <br>
     * @return String
     * @see <a href="https://binance-docs.github.io/apidocs/futures/en/#taker-buy-sell-volume">
     *     https://binance-docs.github.io/apidocs/futures/en/#taker-buy-sell-volume</a>
     */
    public String takerBuySellVol(LinkedHashMap<String, Object> parameters) {
        ParameterChecker.checkParameter(parameters, "symbol", String.class);
        ParameterChecker.checkParameter(parameters, "period", String.class);
        return getRequestHandler().sendPublicRequest(getBaseUrl(), TAKE_BUY_SELL_VOLUME, parameters, HttpMethod.GET, getShowLimitUsage());
    }

    private final String HISTORICAL_BLVT = "/v1/lvtKlines";
    /**
     * The BLVT NAV system is based on Binance Futures, so the endpoint is based on fapi
     * <br><br>
     * GET /v1/lvtKlines
     * <br>
     * https://binance-docs.github.io/apidocs/futures/en/#historical-blvt-nav-kline-candlestick
     * @param
     * parameters LinkedHashedMap of String,Object pair
     *            where String is the name of the parameter and Object is the value of the parameter
     * <br><br>
     * symbol -- mandatory/string -- the trading pair <br>
     * interval -- mandatory/enum -- interval <br>
     * startTime -- optional/long -- Start Time <br>
     * endTime -- optional/long -- End Time <br>
     * limit -- optional/long -- default 500, max 1000 <br>
     * @return String
     * @see <a href="https://binance-docs.github.io/apidocs/futures/en/#historical-blvt-nav-kline-candlestick">
     *     https://binance-docs.github.io/apidocs/futures/en/#historical-blvt-nav-kline-candlestick</a>
     */
    public String historicalBlvt(LinkedHashMap<String, Object> parameters) {
        ParameterChecker.checkParameter(parameters, "symbol", String.class);
        ParameterChecker.checkParameter(parameters, "interval", String.class);
        return getRequestHandler().sendPublicRequest(getProductUrl(), HISTORICAL_BLVT, parameters, HttpMethod.GET, getShowLimitUsage());
    }

    private final String INDEX_INFO = "/v1/indexInfo";
    /**
     * GET /v1/indexInfo
     * <br>
     * https://binance-docs.github.io/apidocs/futures/en/#composite-index-symbol-information
     * @param
     * parameters LinkedHashedMap of String,Object pair
     *            where String is the name of the parameter and Object is the value of the parameter
     * <br><br>
     * symbol -- optional/string -- the trading pair <br>
     * @return String
     * @see <a href="https://binance-docs.github.io/apidocs/futures/en/#composite-index-symbol-information">
     *     https://binance-docs.github.io/apidocs/futures/en/#composite-index-symbol-information</a>
     */
    public String indexInfo(LinkedHashMap<String, Object> parameters) {
        return getRequestHandler().sendPublicRequest(getProductUrl(), INDEX_INFO, parameters, HttpMethod.GET, getShowLimitUsage());
    }

    private final String ASSET_INDEX = "/v1/assetIndex";
    /**
     * asset index for Multi-Assets mode
     * <br><br>
     * GET /v1/assetIndex
     * <br>
     * https://binance-docs.github.io/apidocs/futures/en/#multi-assets-mode-asset-index
     * @param
     * parameters LinkedHashedMap of String,Object pair
     *            where String is the name of the parameter and Object is the value of the parameter
     * <br><br>
     * symbol -- optional/string -- the trading pair <br>
     * @return String
     * @see <a href="https://binance-docs.github.io/apidocs/futures/en/#multi-assets-mode-asset-index">
     *     https://binance-docs.github.io/apidocs/futures/en/#multi-assets-mode-asset-index</a>
     */
    public String assetIndex(LinkedHashMap<String, Object> parameters) {
        return getRequestHandler().sendPublicRequest(getProductUrl(), ASSET_INDEX, parameters, HttpMethod.GET, getShowLimitUsage());
    }

}
