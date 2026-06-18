package org.dromara.northstar.gateway;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.dromara.northstar.common.IHolidayManager;
import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.common.model.GatewayDescription;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public abstract class Gateway {

	/* 连接状态 */
	protected ConnectionState connection = ConnectionState.DISCONNECTED;
	
	/* 网关信息 */
	protected GatewayDescription description;
	
	protected Gateway(GatewayDescription description) {
		this.description = description;
	}
	
	protected void scheduleConnectionTimeoutAction() {
		CompletableFuture.runAsync(() -> {
			if(connection != ConnectionState.CONNECTED) {
				log.warn("[{}] 超时未成功，自动断开", description.getGatewayId());
				disconnect();
			}
		}, CompletableFuture.delayedExecutor(15, TimeUnit.SECONDS));
	}

	/**
	 * 连接
	 */
	public abstract void connect();
	
	/**
	 * 断开
	 */
	public abstract void disconnect();
	
	/**
	 * 获取网关对应的节假日管理器
	 * 默认为空，由网关实现类提供
	 * @return
	 */
	public Optional<IHolidayManager> getHolidayManager() {
		return Optional.empty();
	}

	/**
	 * 计算当前时间是否处于开市时间
	 * 各个网关实现可以自行重写
	 * @return
	 */
	public boolean isTradingTimeWindow() {
		return true;
	}
}
