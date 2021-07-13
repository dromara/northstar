package tech.xuanwu.northstar.service;

import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.engine.event.FastEventEngine;

@Slf4j
public class SMSTradeService {

	private FastEventEngine feEngine;
	
	private Pattern ptn = Pattern.compile("期货");
	
	public SMSTradeService(FastEventEngine feEngine) {
		this.feEngine = feEngine;
	}
	
	public void dispatchMsg(String text) {
//		if() {
//			
//		}
		log.info("收到消息：{}", text);
		
	}
}
