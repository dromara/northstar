package org.dromara.northstar.copilot.tools;

import lombok.Data;

import java.util.function.Function;

import org.dromara.northstar.copilot.annotation.FunctionCall;
import org.dromara.northstar.copilot.annotation.FunctionParameter;
import org.dromara.northstar.copilot.annotation.FunctionRequest;

/**
 * @Author cly
 * @Description TODO
 * @Date 2024/8/12 14:45
 */
@FunctionCall(name = "queryTrainInfo", description = "查询火车是否发车信息")
public class QueryTrainInfoFunction implements Function<QueryTrainInfoFunction.Request, String> {

    @Data
    @FunctionRequest
    public static class Request {
        @FunctionParameter(description = "根据天气的情况进行查询是否发车，此参数为天气的最高气温")
        Integer type;
    }
    public enum Type{
        hao,
        cha
    }

    @Override
    public String apply(Request request) {
        if (request.type > 35) {
            return "天气情况正常，允许发车";
        }
        return "天气情况较差，不允许发车";
    }
    @Data
    public static class Response {
        String orderId;
    }


}
