package com.binance.connector.client.utils;

import com.binance.connector.client.enums.HttpMethod;
import com.binance.connector.client.exceptions.BinanceConnectorException;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

public final class RequestBuilder {
    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");

    private RequestBuilder() {
    }
    public static Request buildPublicRequest(String fullUrl, HttpMethod httpMethod) {
        try {
            final Request request;
            switch (httpMethod) {
                case POST:
                    request = new Request.Builder().url(fullUrl)
                            .post(RequestBody.create("", JSON_TYPE))
                            .build();
                    break;
                case GET:
                    request = new Request.Builder().url(fullUrl)
                            .get()
                            .addHeader("Content-Type", "application/x-www-form-urlencoded")
                            .build();
                    break;
                case PUT:
                    request = new Request.Builder().url(fullUrl)
                            .put(RequestBody.create("", JSON_TYPE))
                            .addHeader("Content-Type", "application/x-www-form-urlencoded")
                            .build();
                    break;
                case DELETE:
                    request = new Request.Builder().url(fullUrl)
                            .delete()
                            .addHeader("Content-Type", "application/x-www-form-urlencoded")
                            .build();
                    break;
                default:
                    throw new BinanceConnectorException("Invalid HTTP method: " + httpMethod);
            }
            return request;
        } catch (IllegalArgumentException e) {
            throw new BinanceConnectorException("Invalid URL: " + e.getMessage());
        }
     }

    public static Request buildApiKeyRequest(String fullUrl, HttpMethod httpMethod, String apiKey) {
        try {
            final Request request;
            switch (httpMethod) {
                case POST:
                    request = new Request.Builder().url(fullUrl)
                            .post(RequestBody.create("", JSON_TYPE))
                            .addHeader("X-MBX-APIKEY", apiKey)
                            .build();
                    break;
                case GET:
                    request = new Request.Builder().url(fullUrl)
                            .get()
                            .addHeader("Content-Type", "application/x-www-form-urlencoded")
                            .addHeader("X-MBX-APIKEY", apiKey)
                            .build();
                    break;
                case PUT:
                    request = new Request.Builder().url(fullUrl)
                            .put(RequestBody.create("", JSON_TYPE))
                            .addHeader("Content-Type", "application/x-www-form-urlencoded")
                            .addHeader("X-MBX-APIKEY", apiKey)
                            .build();
                    break;
                case DELETE:
                    request = new Request.Builder().url(fullUrl)
                            .delete()
                            .addHeader("Content-Type", "application/x-www-form-urlencoded")
                            .addHeader("X-MBX-APIKEY", apiKey)
                            .build();
                    break;
                default:
                    throw new BinanceConnectorException("Invalid HTTP method: " + httpMethod);
            }
            return request;
        } catch (IllegalArgumentException e) {
            throw new BinanceConnectorException("Invalid URL: " + e.getMessage());
        }
    }

    public static Request buildWebsocketRequest(String fullUrl) {
        return new Request.Builder().url(fullUrl).build();
    }
}
