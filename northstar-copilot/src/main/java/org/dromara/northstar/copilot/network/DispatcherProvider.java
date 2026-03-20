package org.dromara.northstar.copilot.network;

import okhttp3.Dispatcher;

/**
 * @Author cly
 * @Description Dispatcher提供器
 * @Date 2024/10/16 23:09
 */
public interface DispatcherProvider {
    Dispatcher getDispatcher();
}
