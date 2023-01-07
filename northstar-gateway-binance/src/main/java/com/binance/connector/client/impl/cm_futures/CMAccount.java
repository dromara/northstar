package com.binance.connector.client.impl.cm_futures;

import com.binance.connector.client.enums.HttpMethod;
import com.binance.connector.client.utils.ParameterChecker;
import java.util.LinkedHashMap;
import com.binance.connector.client.impl.futures.Account;

/**
 * <h2>Coin-Margined Trade Endpoints</h2>
 * All endpoints under the
 * <a href="https://binance-docs.github.io/apidocs/delivery/en/#account-trades-endpoints">Futures Account/Trade Endpoint</a>
 * section of the API documentation will be implemented in this class.
 * <br>
 * Response will be returned in <i>String format</i>.
 */
public class CMAccount extends Account {
    public CMAccount(String productUrl, String apiKey, String secretKey, boolean showLimitUsage) {
        super(productUrl, apiKey, secretKey, showLimitUsage);
    }

    private final String ORDER = "/v1/order";
    /**
     * Order modify function, currently only LIMIT order modification is supported, modified orders will be reordered in the match queue
     * <br><br>
     * PUT /v1/order
     * <br>
     * @param
     * parameters LinkedHashedMap of String,Object pair
     *            where String is the name of the parameter and Object is the value of the parameter
     * <br><br>
     * orderId -- optional/long <br>
     * origClientOrderId -- optional/string <br>
     * symbol - mandatory/string <br>
     * side -- mandatory/enum <br>
     * quantity -- optional/decimal <br>
     * price -- optional/decimal <br>
     * recvWindow -- optional/long <br>
     * @return String
     * @see <a href="https://binance-docs.github.io/apidocs/delivery/en/#modify-order-trade">
     *    https://binance-docs.github.io/apidocs/delivery/en/#modify-order-trade</a>
     */
    public String modifyOrder(LinkedHashMap<String, Object> parameters) {
        ParameterChecker.checkParameter(parameters, "symbol", String.class);
        ParameterChecker.checkParameter(parameters, "side", String.class);
        return getRequestHandler().sendSignedRequest(getProductUrl(), ORDER, parameters, HttpMethod.PUT, getShowLimitUsage());
    }

    private final String ORDER_AMENDMENT = "/v1/orderAmendment";
    /**
     * Get order modification history
     * <br><br>
     * GET /v1/orderAmendment
     * <br>
     * @param
     * parameters LinkedHashedMap of String,Object pair
     *            where String is the name of the parameter and Object is the value of the parameter
     * <br><br>
     * symbol -- mandatory/string <br>
     * orderId -- optional/long <br>
     * origClientOrderId -- optional/string <br>
     * startTime -- optional/long <br>
     * endTime -- optional/long <br>
     * limit -- optional/integer <br>
     * recvWindow -- optional/long <br>
     * @return String
     * @see <a href="https://binance-docs.github.io/apidocs/delivery/en/#get-order-modify-history-user_data">
     *     https://binance-docs.github.io/apidocs/delivery/en/#get-order-modify-history-user_data</a>
     */
    public String orderModifyHistory(LinkedHashMap<String, Object> parameters) {
        ParameterChecker.checkParameter(parameters, "symbol", String.class);
        return getRequestHandler().sendSignedRequest(getProductUrl(), ORDER_AMENDMENT, parameters, HttpMethod.GET, getShowLimitUsage());
    }


    /**
     * Get all open orders on a symbol. Careful when accessing this with no symbol.
     * <br><br>
     * GET /v1/openOrders
     * <br>
     * @param
     * parameters LinkedHashedMap of String,Object pair
     *            where String is the name of the parameter and Object is the value of the parameter
     * <br><br>
     * symbol -- optional/string <br>
     * pair -- optional/string <br>
     * recvWindow -- optional/long <br>
     * @return String
     * @see <a href="https://binance-docs.github.io/apidocs/delivery/en/#current-all-open-orders-user_data">
     *    https://binance-docs.github.io/apidocs/delivery/en/#current-all-open-orders-user_data</a>
     */
    public String currentAllOpenOrders(LinkedHashMap<String, Object> parameters) {
        ParameterChecker.checkOrParameters(parameters, "symbol", "pair");
        return super.currentAllOpenOrders(parameters);
    }

    /**
     * Get all open orders on a symbol. Careful when accessing this with no symbol.
     * <br><br>
     * GET /v1/openOrders
     * <br>
     * @param
     * parameters LinkedHashedMap of String,Object pair
     *            where String is the name of the parameter and Object is the value of the parameter
     * <br><br>
     * symbol -- optional/string <br>
     * pair -- optional/string <br>
     * orderId -- optional/long <br>
     * startTime -- optional/long <br>
     * endTime -- optional/long <br>
     * limit -- optional/integer <br>
     * recvWindow -- optional/long <br>
     * @return String
     * @see <a href="https://binance-docs.github.io/apidocs/delivery/en/#all-orders-user_data">
     *    https://binance-docs.github.io/apidocs/delivery/en/#all-orders-user_data</a>
     */
    public String allOrders(LinkedHashMap<String, Object> parameters) {
        ParameterChecker.checkOrParameters(parameters, "symbol", "pair");
        return super.allOrders(parameters);
    }

    private final String BALANCE = "/v1/balance";
    /**
     * Get Futures Account Balance
     * <br><br>
     * GET /v1/balance
     * <br>
     * @param
     * parameters LinkedHashedMap of String,Object pair
     *            where String is the name of the parameter and Object is the value of the parameter
     * <br><br>
     * recvWindow -- optional/long <br>
     * @return String
     * @see <a href="https://binance-docs.github.io/apidocs/delivery/en/#futures-account-balance-user_data">
     *    https://binance-docs.github.io/apidocs/delivery/en/#futures-account-balance-user_data</a>
     */
    public String futuresAccountBalance(LinkedHashMap<String, Object> parameters) {
        return getRequestHandler().sendSignedRequest(getProductUrl(), BALANCE, parameters, HttpMethod.GET, getShowLimitUsage());
    }

    private final String ACCOUNT_INFORMATION = "/v1/account";
    /**
     * Get current account information. User in single-asset/ multi-assets mode will see different value, see comments in response section for detail.
     * <br><br>
     * GET /v2/account
     * <br>
     * @param
     * parameters LinkedHashedMap of String,Object pair
     *            where String is the name of the parameter and Object is the value of the parameter
     * <br><br>
     * recvWindow -- optional/long <br>
     * @return String
     * @see <a href="https://binance-docs.github.io/apidocs/delivery/en/#account-information-user_data">
     *    https://binance-docs.github.io/apidocs/delivery/en/#account-information-user_data</a>
     */
    public String accountInformation(LinkedHashMap<String, Object> parameters) {
        return getRequestHandler().sendSignedRequest(getProductUrl(), ACCOUNT_INFORMATION, parameters, HttpMethod.GET, getShowLimitUsage());
    }

    private final String POSITION_RISK_V1 = "/v1/positionRisk";
    /**
     * Get current position information.
     * <br><br>
     * GET /v1/positionRisk
     * <br>
     * @param
     * parameters LinkedHashedMap of String,Object pair
     *            where String is the name of the parameter and Object is the value of the parameter
     * <br><br>
     * marginAsset -- optional/string <br>
     * pair -- optional/string <br>
     * recvWindow -- optional/long <br>
     * @return String
     * @see <a href="https://binance-docs.github.io/apidocs/delivery/en/#position-information-user_data">
     *    https://binance-docs.github.io/apidocs/delivery/en/#position-information-user_data</a>
     */
    public String positionInformation(LinkedHashMap<String, Object> parameters) {
        return getRequestHandler().sendSignedRequest(getProductUrl(), POSITION_RISK_V1, parameters, HttpMethod.GET, getShowLimitUsage());
    }

    /**
     * Get trades for a specific account and symbol.
     * <br><br>
     * GET /v1/userTrades
     * <br>
     * @param
     * parameters LinkedHashedMap of String,Object pair
     *            where String is the name of the parameter and Object is the value of the parameter
     * <br><br>
     * symbol -- optional/string <br>
     * pair -- optional/string <br>
     * startTime -- optional/long <br>
     * endTime -- optional/long <br>
     * fromId -- optional/long <br>
     * limit -- optional/integer <br>
     * recvWindow -- optional/long <br>
     * @return String
     * @see <a href="https://binance-docs.github.io/apidocs/delivery/en/#account-trade-list-user_data">
     *    https://binance-docs.github.io/apidocs/delivery/en/#account-trade-list-user_data</a>
     */
    public String accountTradeList(LinkedHashMap<String, Object> parameters) {
        ParameterChecker.checkOrParameters(parameters, "symbol", "pair");
        return super.accountTradeList(parameters);
    }

    /**
     * Notional and Leverage Brackets
     * <br><br>
     * GET /v1/leverageBracket
     * <br>
     * @param
     * parameters LinkedHashedMap of String,Object pair
     *            where String is the name of the parameter and Object is the value of the parameter
     * <br><br>
     * pair -- optional/string <br>
     * recvWindow -- optional/long <br>
     * @return String
     * @see <a href="https://binance-docs.github.io/apidocs/delivery/en/#notional-bracket-for-symbol-user_data">
     *    https://binance-docs.github.io/apidocs/delivery/en/#notional-bracket-for-symbol-user_data</a>
     */
    public String getLeverageBracket(LinkedHashMap<String, Object> parameters) {
        return super.getLeverageBracket(parameters);
    }

    private final String LEVERAGE_BRACKET_PAIR = "/v2/leverageBracket";
    /**
     * Notional and Leverage Brackets
     * <br><br>
     * GET /v1/leverageBracket
     * <br>
     * @param
     * parameters LinkedHashedMap of String,Object pair
     *            where String is the name of the parameter and Object is the value of the parameter
     * <br><br>
     * symbol -- optional/string <br>
     * recvWindow -- optional/long <br>
     * @return String
     * @see <a href="https://binance-docs.github.io/apidocs/delivery/en/#notional-bracket-for-pair-user_data">
     *    https://binance-docs.github.io/apidocs/delivery/en/#notional-bracket-for-pair-user_data</a>
     */
    public String getLeverageBracketForPair(LinkedHashMap<String, Object> parameters) {
        return getRequestHandler().sendSignedRequest(getProductUrl(), LEVERAGE_BRACKET_PAIR, parameters, HttpMethod.GET, getShowLimitUsage());
    }
}
