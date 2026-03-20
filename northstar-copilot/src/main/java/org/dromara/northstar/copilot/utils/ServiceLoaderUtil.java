package org.dromara.northstar.copilot.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.ServiceLoader;

/**
 * @Author cly
 * @Description SPI服务加载类
 * @Date 2024/10/16 23:25
 */
@Slf4j
public class ServiceLoaderUtil {
    public static <T> T load(Class<T> service) {
        ServiceLoader<T> loader = ServiceLoader.load(service);
        for (T impl : loader) {
            log.info("Loaded SPI implementation: {}", impl.getClass().getSimpleName());
            return impl;
        }
        throw new IllegalStateException("No implementation found for " + service.getName());
    }
}
