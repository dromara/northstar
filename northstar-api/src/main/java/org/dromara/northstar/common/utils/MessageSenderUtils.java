package org.dromara.northstar.common.utils;

import java.util.concurrent.atomic.AtomicReference;

import org.dromara.northstar.common.ObjectManager;
import org.dromara.northstar.strategy.IMessageSender;

public class MessageSenderUtils {

	private static final AtomicReference<ObjectManager<IMessageSender>> senderMgrRef = new AtomicReference<>();
	
	public static void setSenderManager(ObjectManager<IMessageSender> senderMgr) {
		senderMgrRef.set(senderMgr);
	}
	
	public static ObjectManager<IMessageSender> getSenderManager(){
		return senderMgrRef.get();
	}
}
