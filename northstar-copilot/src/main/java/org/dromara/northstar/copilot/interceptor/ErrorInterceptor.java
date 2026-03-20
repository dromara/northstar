package org.dromara.northstar.copilot.interceptor;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import org.dromara.northstar.copilot.exception.CommonException;
import org.dromara.northstar.copilot.exception.chain.ErrorHandler;
import org.dromara.northstar.copilot.exception.error.Error;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @Author cly
 * @Description 错误处理器
 * @Date 2024/8/29 14:55
 */
@Slf4j
public class ErrorInterceptor implements Interceptor {
    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        Request original = chain.request();
        Response response = chain.proceed(original);

        // 判断是否为流式响应，流式响应Content-Type为text/event-stream
        if (isStreamingResponse(response)) {
            return response; // 直接返回，不处理流式响应体
        }

        if (!response.isSuccessful() && (response.code() != 100 && response.code() != 101)) {
            ResponseBody responseBody = response.body();
            String errorMsg = getResponseBodyContent(responseBody);

            try {
                JSONObject object = JSON.parseObject(errorMsg);
                if (object == null) {
                    errorMsg = response.code() + " " + response.message();
                    throw new CommonException(errorMsg);
                }
            } catch (Exception e) {
                throw new CommonException(errorMsg);
            }

            ErrorHandler errorHandler = ErrorHandler.getInstance();
            Error error = errorHandler.process(errorMsg);
            log.error("AI服务请求异常：{}", error.getMessage());
            throw new CommonException(error.getMessage());
        } else {

            // 处理腾讯混元部分
            ResponseBody responseBody = response.body();
            String content = getResponseBodyContent(responseBody);
            if (content.contains("Response") && content.contains("Error")) {
                ErrorHandler errorHandler = ErrorHandler.getInstance();
                Error error = errorHandler.process(content);
                log.error("AI服务请求异常：{}", error.getMessage());
                throw new CommonException(error.getMessage());
            }
            // 重新构建响应体，确保内容可用
            ResponseBody newBody = ResponseBody.create(responseBody.contentType(), content);
            return response.newBuilder().body(newBody).build();
        }
    }

    private boolean isStreamingResponse(Response response) {
        MediaType contentType = response.body().contentType();
        return contentType != null && contentType.toString().contains("text/event-stream");
    }

    private String getResponseBodyContent(ResponseBody responseBody) throws IOException {
        if (responseBody == null) return "";
        byte[] contentBytes = responseBody.bytes();
        return new String(contentBytes, StandardCharsets.UTF_8);
    }
}