package tech.xuanwu.northstar.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 交易控制器
 * @author KevinHuangwl
 *
 */
@RestController
@RequestMapping("/trade")
public class TradeOperationController {

	@PostMapping("/submit")
	public String submitOrder() {
		return "123456";
	}
	
	@PostMapping("/cancel")
	public boolean cancelOrder() {
		return true;
	}
	
}
