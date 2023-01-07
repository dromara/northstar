package com.binance.connector.client.impl.cm_futures;

import com.binance.connector.client.impl.futures.UserData; 

/**
 * <h2>Coin-Margined User Data Streams Endpoints</h2>
 * All endpoints under the
 * <a href="https://binance-docs.github.io/apidocs/delivery/en/#user-data-streams">User Data Streams</a>
 * section of the API documentation will be implemented in this class.
 * <br>
 * Response will be returned in <i>String format</i>.
 */
public class CMUserData extends UserData {
    public CMUserData(String productUrl, String apiKey, String secretKey, boolean showLimitUsage) {
        super(productUrl, apiKey, secretKey, showLimitUsage);
    }
}