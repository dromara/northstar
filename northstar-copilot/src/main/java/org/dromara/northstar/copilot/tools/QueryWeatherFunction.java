package org.dromara.northstar.copilot.tools;

import java.util.function.Function;

import org.dromara.northstar.copilot.annotation.FunctionCall;
import org.dromara.northstar.copilot.annotation.FunctionParameter;
import org.dromara.northstar.copilot.annotation.FunctionRequest;

import lombok.Data;
import okhttp3.OkHttpClient;
import okhttp3.Response;

/**
 * @Author cly
 * @Description TODO
 * @Date 2024/8/13 17:40
 */
@FunctionCall(name = "queryWeather", description = "查询目标地点的天气预报")
public class QueryWeatherFunction implements Function<QueryWeatherFunction.Request, String> {
    @Override
    public String apply(Request request) {
        final String key = "S3zzVyAdJjEeB18Gw";
        // https://api.seniverse.com/v3/weather/hourly.json?key=your_api_key&location=beijing&start=0&hours=24
        // https://api.seniverse.com/v3/weather/daily.json?key=your_api_key&location=beijing&start=0&days=5
        String url = String.format("https://api.seniverse.com/v3/weather/%s.json?key=%s&location=%s&days=%d",
                request.type.name(),
                key,
                request.location,
                request.days);


        OkHttpClient client = new OkHttpClient();

        okhttp3.Request http = new okhttp3.Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = client.newCall(http).execute()) {
            if (response.isSuccessful()) {
                // 解析响应体
                return response.body() != null ? response.body().string() : "";
            } else {
                return "获取天气失败 当前天气未知";
            }
        } catch (Exception e) {
            // 处理异常
            e.printStackTrace();
            return "获取天气失败 当前天气未知";
        }
    }

    @Data
    @FunctionRequest
    public static class Request{
        @FunctionParameter(description = "需要查询天气的目标位置, 可以是城市中文名、城市拼音/英文名、省市名称组合、IP 地址、经纬度")
        private String location;
        @FunctionParameter(description = "需要查询未来天气的天数, 最多15日")
        private int days = 15;
        @FunctionParameter(description = "预报的天气类型，daily表示预报多天天气、hourly表示预测当天24天气、now为当前天气实况")
        private Type type;
    }

    public enum Type{
        daily,
        hourly,
        now
    }
}
