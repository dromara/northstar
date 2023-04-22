package org.dromara.northstar.module;

import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.data.IModuleRepository;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.strategy.TradeStrategy;
import org.dromara.northstar.support.log.ModuleLoggerFactory;
import org.dromara.northstar.support.notification.IMessageSenderManager;

public class PlaybackModuleContext extends ModuleContext {
	
	public static final String PLAYBACK_GATEWAY = "回测账户";

	public PlaybackModuleContext(TradeStrategy tradeStrategy, ModuleDescription moduleDescription,
			ModuleRuntimeDescription moduleRtDescription, IContractManager contractMgr, IModuleRepository moduleRepo,
			ModuleLoggerFactory loggerFactory, IMessageSenderManager senderMgr) {
		super(tradeStrategy, moduleDescription, moduleRtDescription, contractMgr, moduleRepo, loggerFactory, senderMgr);
	}

}
