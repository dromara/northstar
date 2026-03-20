package org.dromara.northstar.copilot.utils;

import java.util.concurrent.atomic.AtomicReference;

import org.dromara.northstar.copilot.config.MoonshotConfig;

public class SharedInfoUtils {
	
	private static final AtomicReference<MoonshotConfig> moonshotCfgRef = new AtomicReference<>();

	public static MoonshotConfig getMoonshotConfig() {
		return moonshotCfgRef.get();
	}
	
	public static void setMoonshotConfig(MoonshotConfig cfg) {
		moonshotCfgRef.set(cfg);
	}
}
