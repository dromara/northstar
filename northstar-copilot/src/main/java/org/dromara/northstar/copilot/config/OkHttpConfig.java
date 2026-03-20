package org.dromara.northstar.copilot.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * @Author cly
 * @Description OkHttp配置信息
 * @Date 2024/8/11 0:11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OkHttpConfig {

    private HttpLoggingInterceptor.Level log = HttpLoggingInterceptor.Level.HEADERS;
    private int connectTimeout = 300;
    private int writeTimeout = 300;
    private int readTimeout = 300;
    private int proxyPort = 10809;
    private String proxyHost = "";


}
