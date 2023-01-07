package com.binance.connector.client.impl.futures;

import com.binance.connector.client.enums.HttpMethod;
import com.binance.connector.client.utils.RequestHandler;
import com.binance.connector.client.utils.ParameterChecker;
import java.util.LinkedHashMap;

/**
 * <h2>Portfolio Margin Endpoints</h2>
 * Response will be returned in <i>String format</i>.
 */
public abstract class PortfolioMargin {
    private String productUrl;
    private RequestHandler requestHandler;
    private boolean showLimitUsage;

    public PortfolioMargin(String productUrl, String apiKey, String secretKey, boolean showLimitUsage) {
        this.productUrl = productUrl;
        this.requestHandler = new RequestHandler(apiKey, secretKey);
        this.showLimitUsage = showLimitUsage;
    }

    public String getProductUrl() {
        return this.productUrl;
    }

    public RequestHandler getRequestHandler() {
        return this.requestHandler;
    }

    public boolean getShowLimitUsage() {
        return this.showLimitUsage;
    }

    public void setProductUrl(String productUrl) {
        this.productUrl = productUrl;
    }

    public void setRequestHandler(String apiKey, String secretKey) {
        this.requestHandler = new RequestHandler(apiKey, secretKey);
    }

    public void setShowLimitUsage(boolean showLimitUsage) {
        this.showLimitUsage = showLimitUsage;
    }

    private final String PORTFOLIO_MARGIN_EXCHANGE_INFO = "/v1/pmExchangeInfo";
    public String portfolioMarginExchangeInfo(LinkedHashMap<String, Object> parameters) {
        return requestHandler.sendSignedRequest(productUrl, PORTFOLIO_MARGIN_EXCHANGE_INFO, parameters, HttpMethod.GET, showLimitUsage);
    }

    private final String PORTFOLIO_MARGIN_ACCOUNT_INFO = "/v1/pmAccountInfo";
    /**
     * Get Portfolio Margin current account information.
     * GET /v1/pmAccountInfo
     * <br>
     * @param
     * parameters LinkedHashedMap of String,Object pair
     *            where String is the name of the parameter and Object is the value of the parameter
     * <br><br>
     * asset -- mandatory/string <br>
     * recvWindow -- optional/long <br>
     * @return String
     * @see <a href="https://binance-docs.github.io/apidocs/futures/en/#portfolio-margin-account-information-user_data">
     *     https://binance-docs.github.io/apidocs/futures/en/#portfolio-margin-account-information-user_data</a>
     */
    public String portfolioMarginAccountInfo(LinkedHashMap<String, Object> parameters) {
        ParameterChecker.checkParameter(parameters, "asset", String.class);
        return requestHandler.sendSignedRequest(productUrl, PORTFOLIO_MARGIN_ACCOUNT_INFO, parameters, HttpMethod.GET, showLimitUsage);
    }
}