package com.binance.connector.client.utils;

import com.binance.connector.client.exceptions.BinanceClientException;
import com.binance.connector.client.exceptions.BinanceConnectorException;
import com.binance.connector.client.exceptions.BinanceServerException;
import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONException;
import org.json.JSONObject;

public final class ResponseHandler {
    private static final OkHttpClient client = HttpClientSingleton.getHttpClient();
    private static final int HTTP_STATUS_CODE_400 = 400;
    private static final int HTTP_STATUS_CODE_499 = 499;
    private static final int HTTP_STATUS_CODE_500 = 500;

    private ResponseHandler() {
    }

    public static String handleResponse(Request request, boolean showLimitUsage) {
        try (Response response = client.newCall(request).execute()) {
            if (null == response) {
                throw new BinanceServerException("[ResponseHandler] No response from server");
            }

            String responseAsString = getResponseBodyAsString(response.body());

            if (response.code() >= HTTP_STATUS_CODE_400 && response.code() <= HTTP_STATUS_CODE_499) {
                throw handleErrorResponse(responseAsString, response.code());
            } else if (response.code() >= HTTP_STATUS_CODE_500) {
                throw new BinanceServerException(responseAsString, response.code());
            }

            if (showLimitUsage) {
                return getlimitUsage(response, responseAsString);
            } else {
                return responseAsString;
            }
        } catch (IOException | IllegalStateException e) {
            throw new BinanceConnectorException("[ResponseHandler] OKHTTP Error: " + e.getMessage());
        }
    }

    private static String getlimitUsage(Response response, String resposeBodyAsString) {
        JSONObject json = new JSONObject();
        json.put("x-mbx-used-weight", response.header("x-mbx-used-weight"));
        json.put("x-mbx-used-weight-1m", response.header("x-mbx-used-weight-1m"));
        json.put("data", resposeBodyAsString);

        return json.toString();
    }

    private static BinanceClientException handleErrorResponse(String responseBody, int responseCode) {
        try {
            String errorMsg = JSONParser.getJSONStringValue(responseBody, "msg");
            int errorCode = JSONParser.getJSONIntValue(responseBody, "code");
            return new BinanceClientException(responseBody, errorMsg, responseCode, errorCode);
        } catch (JSONException e) {
            throw new BinanceClientException(responseBody, responseCode);
        }
    }

    private static String getResponseBodyAsString(ResponseBody body) throws IOException {
        if (null != body) {
            return body.string();
        } else {
            return "";
        }
    }
}
