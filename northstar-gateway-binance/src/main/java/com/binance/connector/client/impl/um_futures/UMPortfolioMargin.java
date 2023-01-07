package com.binance.connector.client.impl.um_futures;

import java.util.LinkedHashMap;
import com.binance.connector.client.impl.futures.PortfolioMargin; 

/**
 * <h2>USDⓈ-Margined Portfolio Margin Endpoints</h2>
 * All endpoints under the
 * <a href="https://binance-docs.github.io/apidocs/futures/en/#portfolio-margin-endpoints">PortfolioMargin Endpoint</a>
 * section of the API documentation will be implemented in this class.
 * <br>
 * Response will be returned in <i>String format</i>.
 */
public class UMPortfolioMargin extends PortfolioMargin {
    public UMPortfolioMargin(String productUrl, String apiKey, String secretKey, boolean showLimitUsage) {
        super(productUrl, apiKey, secretKey, showLimitUsage);
    }

    /**
     * Current Portfolio Margin exchange trading rules.
     * GET /v1/pmExchangeInfo
     * <br>
     * @param
     * parameters LinkedHashedMap of String,Object pair
     *            where String is the name of the parameter and Object is the value of the parameter
     * <br><br>
     * symbol -- optional/string <br>
     * @return String
     * @see <a href="https://binance-docs.github.io/apidocs/futures/en/#portfolio-margin-exchange-information">
     *     https://binance-docs.github.io/apidocs/futures/en/#portfolio-margin-exchange-information</a>
     */
    public String portfolioMarginExchangeInfo(LinkedHashMap<String, Object> parameters) {
        return super.portfolioMarginExchangeInfo(parameters);
    }
}