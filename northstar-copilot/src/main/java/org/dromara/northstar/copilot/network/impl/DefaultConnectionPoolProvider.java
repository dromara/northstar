package org.dromara.northstar.copilot.network.impl;

import org.dromara.northstar.copilot.network.ConnectionPoolProvider;

import okhttp3.ConnectionPool;

/**
 * @Author cly
 * @Description ConnectionPool默认实现
 * @Date 2024/10/16 23:11
 */
public class DefaultConnectionPoolProvider implements ConnectionPoolProvider {
    @Override
    public ConnectionPool getConnectionPool() {
        return new ConnectionPool();
    }
}
