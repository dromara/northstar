package org.dromara.northstar.copilot.listener;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * @Author cly
 * @Description RealtimeListener, 用于统一处理WebSocket的事件（Realtime客户端专用）
 * @Date 2024/10/12 16:33
 */
@Slf4j
public abstract class RealtimeListener extends WebSocketListener {

    protected abstract void onOpen(WebSocket webSocket);
    protected abstract void onMessage(ByteString bytes);
    protected abstract void onMessage(String text);
    protected abstract void onFailure();

    @Override
    public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
        log.info("WebSocket Opened: " + response.message());
        this.onOpen(webSocket);
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
        log.info("Receive Byte Message: " + bytes.toString());
        this.onMessage(bytes);
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
        log.info("Receive String Message: " + text);
        this.onMessage(text);
    }

    @Override
    public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
        log.error("WebSocket Error: ", t);
    }

    @Override
    public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
    }

    @Override
    public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        log.info("WebSocket Closed: " + reason);
    }
}
