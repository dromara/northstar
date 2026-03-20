package org.dromara.northstar.copilot.network.impl;

import org.dromara.northstar.copilot.network.DispatcherProvider;

import okhttp3.Dispatcher;

/**
 * @Author cly
 * @Description Dispatcher默认实现
 * @Date 2024/10/16 23:11
 */
public class DefaultDispatcherProvider implements DispatcherProvider {
    @Override
    public Dispatcher getDispatcher() {
        return new Dispatcher();
    }
}
