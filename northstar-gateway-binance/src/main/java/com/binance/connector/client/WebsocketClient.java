package com.binance.connector.client;

import com.binance.connector.client.utils.WebSocketCallback;
import java.util.ArrayList;

public interface WebsocketClient {
    int symbolTicker(String symbol, WebSocketCallback onMessageCallback);
    int symbolTicker(String symbol, WebSocketCallback onOpenCallback, WebSocketCallback onMessageCallback, WebSocketCallback onClosingCallback, WebSocketCallback onFailureCallback);
    int klineStream(String symbol, String interval, WebSocketCallback onMessageCallback);
    int klineStream(String symbol, String interval, WebSocketCallback onOpenCallback, WebSocketCallback onMessageCallback, WebSocketCallback onClosingCallback, WebSocketCallback onFailureCallback);
    int aggTradeStream(String symbol, WebSocketCallback onMessageCallback);
    int aggTradeStream(String symbol, WebSocketCallback onOpenCallback, WebSocketCallback onMessageCallback, WebSocketCallback onClosingCallback, WebSocketCallback onFailureCallback);
    int miniTickerStream(String symbol, WebSocketCallback onMessageCallback);
    int miniTickerStream(String symbol, WebSocketCallback onOpenCallback, WebSocketCallback onMessageCallback, WebSocketCallback onClosingCallback, WebSocketCallback onFailureCallback);
    int allTickerStream(WebSocketCallback onMessageCallback);
    int allTickerStream(WebSocketCallback onOpenCallback, WebSocketCallback onMessageCallback, WebSocketCallback onClosingCallback, WebSocketCallback onFailureCallback);
    int allMiniTickerStream(WebSocketCallback onMessageCallback);
    int allMiniTickerStream(WebSocketCallback onOpenCallback, WebSocketCallback onMessageCallback, WebSocketCallback onClosingCallback, WebSocketCallback onFailureCallback);
    int bookTicker(String symbol, WebSocketCallback onMessageCallback);
    int bookTicker(String symbol, WebSocketCallback onOpenCallback, WebSocketCallback onMessageCallback, WebSocketCallback onClosingCallback, WebSocketCallback onFailureCallback);
    int allBookTickerStream(WebSocketCallback onMessageCallback);
    int allBookTickerStream(WebSocketCallback onOpenCallback, WebSocketCallback onMessageCallback, WebSocketCallback onClosingCallback, WebSocketCallback onFailureCallback);
    int partialDepthStream(String symbol, int levels, int speed, WebSocketCallback onMessageCallback);
    int partialDepthStream(String symbol, int levels, int speed, WebSocketCallback onOpenCallback, WebSocketCallback onMessageCallback, WebSocketCallback onClosingCallback, WebSocketCallback onFailureCallback);
    int diffDepthStream(String symbol, int speed, WebSocketCallback onMessageCallback);
    int diffDepthStream(String symbol, int speed, WebSocketCallback onOpenCallback, WebSocketCallback onMessageCallback, WebSocketCallback onClosingCallback, WebSocketCallback onFailureCallback);
    int combineStreams(ArrayList<String> streams, WebSocketCallback onMessageCallback);
    int combineStreams(ArrayList<String> streams, WebSocketCallback onOpenCallback, WebSocketCallback onMessageCallback, WebSocketCallback onClosingCallback, WebSocketCallback onFailureCallback);
    int listenUserStream(String listenKey, WebSocketCallback onMessageCallback);
    int listenUserStream(String listenKey, WebSocketCallback onOpenCallback, WebSocketCallback onMessageCallback, WebSocketCallback onClosingCallback, WebSocketCallback onFailureCallback);
    void closeConnection(int streamId);
    void closeAllConnections();
    int markPriceStream(String symbol, int speed, WebSocketCallback onMessageCallback);
    int markPriceStream(String symbol, int speed, WebSocketCallback onOpenCallback, WebSocketCallback onMessageCallback, WebSocketCallback onClosingCallback, WebSocketCallback onFailureCallback);
    int continuousKlineStream(String pair, String interval, String contractType, WebSocketCallback onMessageCallback);
    int continuousKlineStream(String pair, String interval, String contractType, WebSocketCallback onOpenCallback, WebSocketCallback onMessageCallback, WebSocketCallback onClosingCallback, WebSocketCallback onFailureCallback);
    int forceOrderStream(String symbol, WebSocketCallback onMessageCallback);
    int forceOrderStream(String symbol, WebSocketCallback onOpenCallback, WebSocketCallback onMessageCallback, WebSocketCallback onClosingCallback, WebSocketCallback onFailureCallback);
    int allForceOrderStream(WebSocketCallback onMessageCallback);
    int allForceOrderStream(WebSocketCallback onOpenCallback, WebSocketCallback onMessageCallback, WebSocketCallback onClosingCallback, WebSocketCallback onFailureCallback);
}