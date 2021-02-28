package tech.xuanwu.northstar.trader.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.trader.domain.strategy.SMSInstructionStrategy;

@Slf4j
@Api(tags = "短信指令跟进策略")
@RestController
public class SMSReceivingController {
	
	@Autowired
	private SMSInstructionStrategy strategy;

	@PostMapping("/sms")
	public void onReceiveSMS(@RequestBody String msg) {
		log.info("收到短信指令：{}", msg);
		strategy.react(msg);
	}
}
