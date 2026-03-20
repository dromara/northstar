package org.dromara.northstar.common.utils;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 主要用于一些内部回环调用的场景，例如Copilot的FunctionCall
 */
public class InternalLoopCallUtils {

	private static final Set<UUID> randomKeySet = new HashSet<>();
	
	public static synchronized UUID produce() {
		UUID uuid = UUID.randomUUID();
		randomKeySet.add(uuid);
		// 自动清理，防止大模型接口异常后，此处出现内存泄漏
		CompletableFuture.runAsync(() -> {
			randomKeySet.remove(uuid);
		}, CompletableFuture.delayedExecutor(3, TimeUnit.MINUTES));
		return uuid;
	}
	
	public static synchronized boolean consume(UUID uuid) {
		return randomKeySet.remove(uuid);
	}
	
}
