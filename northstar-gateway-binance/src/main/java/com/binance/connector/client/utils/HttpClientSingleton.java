package com.binance.connector.client.utils;

import okhttp3.OkHttpClient;

public final class HttpClientSingleton {
    private static final OkHttpClient httpClient = new OkHttpClient();

    private HttpClientSingleton() {
    }

    public static OkHttpClient getHttpClient() {
        return httpClient;
    }
}
