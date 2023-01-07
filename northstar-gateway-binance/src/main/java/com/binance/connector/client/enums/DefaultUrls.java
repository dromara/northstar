package com.binance.connector.client.enums;

public final class DefaultUrls {
    public static final String TESTNET_URL = "https://testnet.binancefuture.com";
    public static final String TESTNET_WSS_URL = "wss://stream.binancefuture.com";
    //USD-M Futures
    public static final String USDM_PROD_URL = "https://fapi.binance.com";
    public static final String USDM_WS_URL = "wss://fstream.binance.com";
    //COIN-M Futures
    public static final String COINM_PROD_URL = "https://dapi.binance.com";
    public static final String COINM_WS_URL = "wss://dstream.binance.com";
    private DefaultUrls() {
    }
}
