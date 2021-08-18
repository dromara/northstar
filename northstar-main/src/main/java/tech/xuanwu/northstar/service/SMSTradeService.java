package tech.xuanwu.northstar.service;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.engine.event.FastEventEngine;

@Slf4j
public class SMSTradeService {

	private FastEventEngine feEngine;
	
	
	public SMSTradeService(FastEventEngine feEngine) {
		this.feEngine = feEngine;
	}
	
	public void dispatchMsg(String text) {
		log.info("收到消息：{}", text);
		if(!text.contains("期货")) {
			return;	
		}
		feEngine.emitEvent(NorthstarEventType.EXT_MSG, text);
	}
}
