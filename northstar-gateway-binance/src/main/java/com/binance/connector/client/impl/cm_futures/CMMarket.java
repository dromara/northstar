package com.binance.connector.client.impl.cm_futures;

import com.binance.connector.client.enums.HttpMethod;
import com.binance.connector.client.utils.ParameterChecker;
import java.util.LinkedHashMap;
import com.binance.connector.client.impl.futures.Market;

/**
 * <h2>Coin-Margined Market Endpoints</h2>
 * All endpoints under the
 * <a href="https://binance-docs.github.io/apidocs/delivery/en/#market-data-endpoints">Market Data Endpoint</a>
 * section of the API documentation will be implemented in this class.
 * <br>
 * Response will be returned in <i>String format</i>.
 */
public class CMMarket extends Market {
    public CMMarket(String productUrl, String baseUrl, String apiKey, boolean showLimitUsage) {
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
     * pair -- optional/string -- the trading pair <br>
     * @return String
     * @see <a href="https://binance-docs.github.io/apidocs/delivery/en/#index-price-and-mark-price">
     *     https://binance-docs.github.io/apidocs/delivery/en/#index-price-and-mark-price</a>
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
     * pair -- optional/string -- the trading pair <br>
     * @return String
     * @see <a href="https://binance-docs.github.io/apidocs/delivery/en/#24hr-ticker-price-change-statistics">
     *     https://binance-docs.github.io/apidocs/delivery/en/#24hr-ticker-price-change-statistics</a>
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
     * pair -- optional/string -- the trading pair <br>
     * @return String
     * @see <a href="https://binance-docs.github.io/apidocs/delivery/en/#symbol-price-ticker">
     *     https://binance-docs.github.io/apidocs/delivery/en/#symbol-price-ticker</a>
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
     * pair -- optional/string -- the trading pair (Only applicable in COIN-M Futures) <br>
     * @return String
     * @see <a href="https://binance-docs.github.io/apidocs/delivery/en/#symbol-order-book-ticker">
     *     https://binance-docs.github.io/apidocs/delivery/en/#symbol-order-book-ticker</a>
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
     * pair -- mandatory/string -- the trading pair <br>
     * period -- mandatory/enum -- "5m","15m","30m","1h","2h","4h","6h","12h","1d" <br>
     * limit -- optional/long -- default 30, max 500 <br>
     * startTime -- optional/long -- Start Time <br>
     * endTime -- optional/long -- End Time <br>
     * @return String
     * @see <a href="https://binance-docs.github.io/apidocs/futures/en/#open-interest-statistics">
     *     https://binance-docs.github.io/apidocs/futures/en/#open-interest-statistics</a>
     */
    public String openInterestStatistics(LinkedHashMap<String, Object> parameters) {
        ParameterChecker.checkParameter(parameters, "pair", String.class);
        ParameterChecker.checkParameter(parameters, "period", String.class);
        return super.openInterestStatistics(parameters);
    }

    /**
     * Top Trader Long/Short Ratio (Positions)
     * <br><br>
     * GET /futures/data/topLongShortPositionRatio
     * <br>
     * https://binance-docs.github.io/apidocs/delivery/en/#top-trader-long-short-ratio-positions
     * @param
     * parameters LinkedHashedMap of String,Object pair
     *            where String is the name of the parameter and Object is the value of the parameter
     * <br><br>
     * pair -- mandatory/string -- the trading pair <br>
     * period -- mandatory/enum -- "5m","15m","30m","1h","2h","4h","6h","12h","1d" <br>
     * limit -- optional/long -- default 30, max 500 <br>
     * startTime -- optional/long -- Start Time <br>
     * endTime -- optional/long -- End Time <br>
     * @return String
     * @see <a href="https://binance-docs.github.io/apidocs/delivery/en/#top-trader-long-short-ratio-positions">
     *     https://binance-docs.github.io/apidocs/delivery/en/#top-trader-long-short-ratio-positions</a>
     */
    public String topTraderLongShortPos(LinkedHashMap<String, Object> parameters) {
        ParameterChecker.checkParameter(parameters, "pair", String.class);
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
     * pair -- mandatory/string -- the trading pair <br>
     * period -- mandatory/enum -- "5m","15m","30m","1h","2h","4h","6h","12h","1d" <br>
     * limit -- optional/long -- default 30, max 500 <br>
     * startTime -- optional/long -- Start Time <br>
     * endTime -- optional/long -- End Time <br>
     * @return String
     * @see <a href="https://binance-docs.github.io/apidocs/delivery/en/#top-trader-long-short-ratio-accounts">
     *     https://binance-docs.github.io/apidocs/delivery/en/#top-trader-long-short-ratio-accounts</a>
     */
    public String topTraderLongShortAccs(LinkedHashMap<String, Object> parameters) {
        ParameterChecker.checkParameter(parameters, "pair", String.class);
        ParameterChecker.checkParameter(parameters, "period", String.class);
        return super.topTraderLongShortAccs(parameters);
    }

    /**
     * Long/Short Ratio
     * <br><br>
     * GET /futures/data/globalLongShortAccountRatio
     * <br>
     * https://binance-docs.github.io/apidocs/delivery/en/#long-short-ratio
     * @param
     * parameters LinkedHashedMap of String,Object pair
     *            where String is the name of the parameter and Object is the value of the parameter
     * <br><br>
     * pair -- mandatory/string -- the trading pair <br>
     * period -- mandatory/enum -- "5m","15m","30m","1h","2h","4h","6h","12h","1d" <br>
     * limit -- optional/long -- default 30, max 500 <br>
     * startTime -- optional/long -- Start Time <br>
     * endTime -- optional/long -- End Time <br>
     * @return String
     * @see <a href="https://binance-docs.github.io/apidocs/delivery/en/#long-short-ratio">
     *     https://binance-docs.github.io/apidocs/delivery/en/#long-short-ratio</a>
     */
    public String longShortRatio(LinkedHashMap<String, Object> parameters) {
        ParameterChecker.checkParameter(parameters, "pair", String.class);
        ParameterChecker.checkParameter(parameters, "period", String.class);
        return super.longShortRatio(parameters);
    }

    private final String BASIS = "/futures/data/basis";
    /**
     * For COIN-M Futures Only
     * <br><br>
     * GET /futures/data/basis
     * <br>
     * https://binance-docs.github.io/apidocs/delivery/en/#basis
     * @param
     * parameters LinkedHashedMap of String,Object pair
     *            where String is the name of the parameter and Object is the value of the parameter
     * <br><br>
     * pair -- mandatory/string -- the trading pair <br>
     * contractType -- mandatory/enum -- CURRENT_QUARTER, NEXT_QUARTER, PERPETUAL
     * period -- mandatory/enum -- "5m","15m","30m","1h","2h","4h","6h","12h","1d"
     * limit -- optional/long -- Default 30,Max 500
     * startTime -- optional/long
     * endTime -- optional/long
     * @return String
     * @see <a href="https://binance-docs.github.io/apidocs/delivery/en/#basis">
     *     https://binance-docs.github.io/apidocs/delivery/en/#basis</a>
     */
    public String basis(LinkedHashMap<String, Object> parameters) {
        ParameterChecker.checkParameter(parameters, "pair", String.class);
        ParameterChecker.checkParameter(parameters, "contractType", String.class);
        ParameterChecker.checkParameter(parameters, "period", String.class);
        return getRequestHandler().sendPublicRequest(getBaseUrl(), BASIS, parameters, HttpMethod.GET, getShowLimitUsage());
    }
}
