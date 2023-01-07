package com.binance.connector.client.utils;

import com.binance.connector.client.enums.HttpMethod;
import com.binance.connector.client.enums.RequestType;
import com.binance.connector.client.exceptions.BinanceConnectorException;
import java.util.LinkedHashMap;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestHandler {
    private final String apiKey;
    private final String secretKey;
    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);

    public RequestHandler(String apiKey) {
        this.apiKey = apiKey;
        this.secretKey = null;
    }

    public RequestHandler(String apiKey, String secretKey) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
    }

    /**
     * Build request based on request type and send the requests to server.
     * @param baseUrl base url
     * @param urlPath url path
     * @param signature the signature
     * @param parameters parameters
     * @param httpMethod https method
     * @param requestType request type
     * @return String - response from server
     */

    private String sendApiRequest(String baseUrl, String urlPath, String signature, LinkedHashMap<String, Object> parameters,
                                  HttpMethod httpMethod, RequestType requestType, boolean showLimitUsage) {
        String fullUrl = UrlBuilder.buildFullUrl(baseUrl, urlPath, parameters, signature);
        logger.info("{} {}", httpMethod, fullUrl);
        Request request;
        switch (requestType) {
            case PUBLIC:
                request = RequestBuilder.buildPublicRequest(fullUrl, httpMethod);
                break;
            case WITH_API_KEY:
            case SIGNED:
                request = RequestBuilder.buildApiKeyRequest(fullUrl, httpMethod, apiKey);
                break;
            default:
                throw new BinanceConnectorException("[RequestHandler] Invalid request type: " + requestType);
        }
        return ResponseHandler.handleResponse(request, showLimitUsage);
    }

    public String sendPublicRequest(String baseUrl, String urlPath, LinkedHashMap<String, Object> parameters,
                                    HttpMethod httpMethod, boolean showLimitUsage) {
        return sendApiRequest(baseUrl, urlPath, null, parameters, httpMethod, RequestType.PUBLIC, showLimitUsage);
    }

    public String sendWithApiKeyRequest(String baseUrl, String urlPath, LinkedHashMap<String, Object> parameters,
                                        HttpMethod httpMethod, boolean showLimitUsage) {
        if (null == apiKey || apiKey.isEmpty()) {
            throw new BinanceConnectorException("[RequestHandler] API key cannot be null or empty!");
        }
        return sendApiRequest(baseUrl, urlPath, null, parameters, httpMethod, RequestType.WITH_API_KEY, showLimitUsage);
    }

    public String sendSignedRequest(String baseUrl, String urlPath, LinkedHashMap<String, Object> parameters,
                                    HttpMethod httpMethod, boolean showLimitUsage) {
        if (null == secretKey || secretKey.isEmpty() || null == apiKey || apiKey.isEmpty()) {
            throw new BinanceConnectorException("[RequestHandler] Secret key/API key cannot be null or empty!");
        }
        parameters.put("timestamp", UrlBuilder.buildTimestamp());
        String queryString = UrlBuilder.joinQueryParameters(parameters);
        String signature = SignatureGenerator.getSignature(queryString, secretKey);
        return sendApiRequest(baseUrl, urlPath, signature, parameters, httpMethod, RequestType.SIGNED, showLimitUsage);
    }
}
