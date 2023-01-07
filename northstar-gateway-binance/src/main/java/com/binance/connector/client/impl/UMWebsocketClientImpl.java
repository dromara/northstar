package com.binance.connector.client.impl;

import com.binance.connector.client.enums.DefaultUrls;
import com.binance.connector.client.utils.RequestBuilder;
import com.binance.connector.client.utils.WebSocketCallback;
import com.binance.connector.client.utils.ParameterChecker;
import okhttp3.Request;

/**
 * <h2>USDⓈ-M  Websocket Streams</h2>
 * All stream endpoints under the
 * <a href="https://binance-docs.github.io/apidocs/futures/en/#websocket-market-streams"> Websocket Market Streams</a> and
 * <a href="https://binance-docs.github.io/apidocs/futures/en/#user-data-streams"> User Data Streams</a>
 * section of the API documentation will be implemented in this class.
 * <br>
 * Response will be returned as callback.
 */
public class UMWebsocketClientImpl extends WebsocketClientImpl {

    public UMWebsocketClientImpl() {
        super(DefaultUrls.USDM_WS_URL);
    }

    public UMWebsocketClientImpl(String baseUrl) {
        super(baseUrl);
    }

    /**
     * Mark price and funding rate for all symbols pushed every 3 seconds or every second.
     * <br><br>
     * &lt;symbol&gt;@markPrice or &lt;symbol&gt;@markPrice@1s
     * <br><br>
     * Update Speed: 3000ms or 1000ms
     *
     * @param speed speed in seconds, can be 1 or 3
     * @param onMessageCallback onMessageCallback
     * @return int - Connection ID
     * @see <a href="https://binance-docs.github.io/apidocs/futures/en/#mark-price-stream-for-all-market">
     * https://binance-docs.github.io/apidocs/futures/en/#mark-price-stream-for-all-market</a>
     */
    public int allMarkPriceStream(int speed, WebSocketCallback onMessageCallback) {
        return allMarkPriceStream(speed, getNoopCallback(), onMessageCallback, getNoopCallback(), getNoopCallback());
    }

    /**
     * Same as {@link #allMarkPriceStream(int, WebSocketCallback)} plus accepts callbacks for all major websocket connection events.
     *
     * @param speed speed in seconds, can be 1 or 3
     * @param onOpenCallback onOpenCallback
     * @param onMessageCallback onMessageCallback
     * @param onClosingCallback onClosingCallback
     * @param onFailureCallback onFailureCallback
     * @return int - Connection ID
     */
    public int allMarkPriceStream(int speed, WebSocketCallback onOpenCallback, WebSocketCallback onMessageCallback, WebSocketCallback onClosingCallback, WebSocketCallback onFailureCallback) {
        Request request = null;
        final int defaultSpeed = 3;
        if (speed == defaultSpeed) {
            request = RequestBuilder.buildWebsocketRequest(String.format("%s/ws/!markPrice@arr", getBaseUrl()));
        } else {
            request = RequestBuilder.buildWebsocketRequest(String.format("%s/ws/!markPrice@arr@%ss", getBaseUrl(), speed));
        }
        return super.createConnection(onOpenCallback, onMessageCallback, onClosingCallback, onFailureCallback, request);
    }

    /**
     * Composite index information for index symbols pushed every second.
     * <br><br>
     * &lt;symbol&gt;@compositeIndex
     * <br><br>
     * Update Speed: 1000ms
     *
     * @param symbol trading symbol
     * @param onMessageCallback onMessageCallback
     * @return int - Connection ID
     * @see <a href="https://binance-docs.github.io/apidocs/futures/en/#composite-index-symbol-information-streams">
     * https://binance-docs.github.io/apidocs/futures/en/#composite-index-symbol-information-streams</a>
     */
    public int compositeIndexSymbolInfo(String symbol, WebSocketCallback onMessageCallback) {
        ParameterChecker.checkParameterType(symbol, String.class, "symbol");
        return compositeIndexSymbolInfo(symbol, getNoopCallback(), onMessageCallback, getNoopCallback(), getNoopCallback());
    }

    /**
     * Same as {@link #compositeIndexSymbolInfo(String, WebSocketCallback)} plus accepts callbacks for all major websocket connection events.
     *
     * @param symbol trading symbol
     * @param onOpenCallback onOpenCallback
     * @param onMessageCallback onMessageCallback
     * @param onClosingCallback onClosingCallback
     * @param onFailureCallback onFailureCallback
     * @return int - Connection ID
     */
    public int compositeIndexSymbolInfo(String symbol, WebSocketCallback onOpenCallback, WebSocketCallback onMessageCallback, WebSocketCallback onClosingCallback, WebSocketCallback onFailureCallback) {
        ParameterChecker.checkParameterType(symbol, String.class, "symbol");
        Request request = RequestBuilder.buildWebsocketRequest(String.format("%s/ws/%s@compositeIndex", getBaseUrl(), symbol.toLowerCase()));
        return createConnection(onOpenCallback, onMessageCallback, onClosingCallback, onFailureCallback, request);
    }

}
