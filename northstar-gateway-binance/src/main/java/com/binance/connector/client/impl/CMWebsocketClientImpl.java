package com.binance.connector.client.impl;

import com.binance.connector.client.enums.DefaultUrls;
import com.binance.connector.client.utils.RequestBuilder;
import com.binance.connector.client.utils.WebSocketCallback;
import com.binance.connector.client.utils.ParameterChecker;
import okhttp3.Request;

/**
 * <h2>COIN-M Websocket Streams</h2>
 * All stream endpoints under the
 * <a href="https://binance-docs.github.io/apidocs/delivery/en/#websocket-market-streams"> Websocket Market Streams</a> and
 * <a href="https://binance-docs.github.io/apidocs/delivery/en/#user-data-streams"> User Data Streams</a>
 * section of the API documentation will be implemented in this class.
 * <br>
 * Response will be returned as callback.
 */
public class CMWebsocketClientImpl extends WebsocketClientImpl {
    public CMWebsocketClientImpl() {
        super(DefaultUrls.COINM_WS_URL);
    }

    public CMWebsocketClientImpl(String baseUrl) {
        super(baseUrl);
    }

    /**
     * Index Price Stream
     * <br><br>
     * &lt;pair&gt;@indexPrice or &lt;pair&gt;@indexPrice@1s
     * <br><br>
     * Update Speed: 3000ms or 1000ms
     *
     * @param pair trading pair
     * @param speed speed in seconds, can be 1 or 3
     * @param onMessageCallback onMessageCallback
     * @return int - Connection ID
     * @see <a href="https://binance-docs.github.io/apidocs/delivery/en/#index-price-stream">
     * https://binance-docs.github.io/apidocs/delivery/en/#index-price-streams</a>
     */
    public int indexPriceStream(String pair, int speed, WebSocketCallback onMessageCallback) {
        ParameterChecker.checkParameterType(pair, String.class, "pair");
        return indexPriceStream(pair, speed, getNoopCallback(), onMessageCallback, getNoopCallback(), getNoopCallback());
    }

    /**
     * Same as {@link #indexPriceStream(String, int, WebSocketCallback)} plus accepts callbacks for all major websocket connection events.
     *
     * @param pair trading pair
     * @param speed speed in seconds, can be 1 or 3
     * @param onOpenCallback onOpenCallback
     * @param onMessageCallback onMessageCallback
     * @param onClosingCallback onClosingCallback
     * @param onFailureCallback onFailureCallback
     * @return int - Connection ID
     */
    public int indexPriceStream(String pair, int speed, WebSocketCallback onOpenCallback, WebSocketCallback onMessageCallback, WebSocketCallback onClosingCallback, WebSocketCallback onFailureCallback) {
        ParameterChecker.checkParameterType(pair, String.class, "pair");
        Request request = null;
        final int defaultSpeed = 3;
        if (speed == defaultSpeed) {
            request = RequestBuilder.buildWebsocketRequest(String.format("%s/ws/%s@indexPrice", getBaseUrl(), pair.toLowerCase(), speed));
        } else {
            request = RequestBuilder.buildWebsocketRequest(String.format("%s/ws/%s@indexPrice@%ss", getBaseUrl(), pair.toLowerCase(), speed));
        }
        return super.createConnection(onOpenCallback, onMessageCallback, onClosingCallback, onFailureCallback, request);
    }

    /**
     * Mark price and funding rate for a single pair pushed every 3 seconds or every second.
     * <br><br>
     * &lt;pair&gt;@markPrice or &lt;pair&gt;@markPrice@1s
     * <br><br>
     * Update Speed: 3000ms or 1000ms
     *
     * @param pair trading pair
     * @param speed speed in seconds, can be 1 or 3
     * @param onMessageCallback onMessageCallback
     * @return int - Connection ID
     * @see <a href="https://binance-docs.github.io/apidocs/delivery/en/#mark-price-of-all-symbols-of-a-pair">
     * https://binance-docs.github.io/apidocs/delivery/en/#mark-price-of-all-symbols-of-a-pair</a>
     */
    public int markPriceSymbolsPairStream(String pair, int speed, WebSocketCallback onMessageCallback) {
        ParameterChecker.checkParameterType(pair, String.class, "pair");
        return markPriceSymbolsPairStream(pair, speed, getNoopCallback(), onMessageCallback, getNoopCallback(), getNoopCallback());
    }

    /**
     * Same as {@link #markPriceSymbolsPairStream(String, int, WebSocketCallback)} plus accepts callbacks for all major websocket connection events.
     *
     * @param pair trading pair
     * @param speed speed in seconds, can be 1 or 3
     * @param onOpenCallback onOpenCallback
     * @param onMessageCallback onMessageCallback
     * @param onClosingCallback onClosingCallback
     * @param onFailureCallback onFailureCallback
     * @return int - Connection ID
     */
    public int markPriceSymbolsPairStream(String pair, int speed, WebSocketCallback onOpenCallback, WebSocketCallback onMessageCallback, WebSocketCallback onClosingCallback, WebSocketCallback onFailureCallback) {
        ParameterChecker.checkParameterType(pair, String.class, "pair");
        Request request = null;
        final int defaultSpeed = 3;
        if (speed == defaultSpeed) {
            request = RequestBuilder.buildWebsocketRequest(String.format("%s/ws/%s@markPrice", getBaseUrl(), pair.toLowerCase()));
        } else {
            request = RequestBuilder.buildWebsocketRequest(String.format("%s/ws/%s@markPrice@%ss", getBaseUrl(), pair.toLowerCase(), speed));
        }
        return createConnection(onOpenCallback, onMessageCallback, onClosingCallback, onFailureCallback, request);
    }

    /**
     * The Kline/Candlestick Stream push updates to the current klines/candlestick every 250 milliseconds (if existing).
     * <br><br>
     * &lt;pair&gt;@indexPriceKline_&lt;interval&gt;
     * <br><br>
     * Update Speed: 250ms
     *
     * @param pair trading pair
     * @param interval kline interval - 1m 3m 5m 15m 30m 1h 2h 4h 6h 8h 12h 1d 3d 1w 1M
     * @param onMessageCallback onMessageCallback
     * @return int - Connection ID
     * @see <a href="https://binance-docs.github.io/apidocs/delivery/en/#index-kline-candlestick-streams">
     * https://binance-docs.github.io/apidocs/delivery/en/#index-kline-candlestick-streams</a>
     */
    public int indexKlineCandlestick(String pair, String interval, WebSocketCallback onMessageCallback) {
        ParameterChecker.checkParameterType(pair, String.class, "pair");
        return indexKlineCandlestick(pair, interval, getNoopCallback(), onMessageCallback, getNoopCallback(), getNoopCallback());
    }

    /**
     * Same as {@link #indexKlineCandlestick(String, String, WebSocketCallback)} plus accepts callbacks for all major websocket connection events.
     *
     * @param pair trading pair
     * @param interval kline interval - 1m 3m 5m 15m 30m 1h 2h 4h 6h 8h 12h 1d 3d 1w 1M
     * @param onOpenCallback onOpenCallback
     * @param onMessageCallback onMessageCallback
     * @param onClosingCallback onClosingCallback
     * @param onFailureCallback onFailureCallback
     * @return int - Connection ID
     */
    public int indexKlineCandlestick(String pair, String interval, WebSocketCallback onOpenCallback, WebSocketCallback onMessageCallback, WebSocketCallback onClosingCallback, WebSocketCallback onFailureCallback) {
        ParameterChecker.checkParameterType(pair, String.class, "pair");
        Request request = RequestBuilder.buildWebsocketRequest(String.format("%s/ws/%s@indexPriceKline_%s", getBaseUrl(), pair.toLowerCase(), interval));
        return createConnection(onOpenCallback, onMessageCallback, onClosingCallback, onFailureCallback, request);
    }

    /**
     * The Kline/Candlestick Stream push updates to the current klines/candlestick every 250 milliseconds (if existing).
     * <br><br>
     * &lt;symbol&gt;@markPriceKline_&lt;interval&gt;
     * <br><br>
     * Update Speed: 250ms
     *
     * @param symbol trading symbol
     * @param interval kline interval - 1m 3m 5m 15m 30m 1h 2h 4h 6h 8h 12h 1d 3d 1w 1M
     * @param onMessageCallback onMessageCallback
     * @return int - Connection ID
     * @see <a href="https://binance-docs.github.io/apidocs/delivery/en/#mark-price-kline-candlestick-streams">
     * https://binance-docs.github.io/apidocs/delivery/en/#mark-price-kline-candlestick-streams</a>
     */
    public int markKlineCandlestick(String symbol, String interval, WebSocketCallback onMessageCallback) {
        ParameterChecker.checkParameterType(symbol, String.class, "symbol");
        return markKlineCandlestick(symbol, interval, getNoopCallback(), onMessageCallback, getNoopCallback(), getNoopCallback());
    }

    /**
     * Same as {@link #markKlineCandlestick(String, String, WebSocketCallback)} plus accepts callbacks for all major websocket connection events.
     *
     * @param symbol trading symbol
     * @param interval kline interval - 1m 3m 5m 15m 30m 1h 2h 4h 6h 8h 12h 1d 3d 1w 1M
     * @param onOpenCallback onOpenCallback
     * @param onMessageCallback onMessageCallback
     * @param onClosingCallback onClosingCallback
     * @param onFailureCallback onFailureCallback
     * @return int - Connection ID
     */
    public int markKlineCandlestick(String symbol, String interval, WebSocketCallback onOpenCallback, WebSocketCallback onMessageCallback, WebSocketCallback onClosingCallback, WebSocketCallback onFailureCallback) {
        ParameterChecker.checkParameterType(symbol, String.class, "symbol");
        Request request = RequestBuilder.buildWebsocketRequest(String.format("%s/ws/%s@markPriceKline_%s", getBaseUrl(), symbol.toLowerCase(), interval));
        return createConnection(onOpenCallback, onMessageCallback, onClosingCallback, onFailureCallback, request);
    }
}
