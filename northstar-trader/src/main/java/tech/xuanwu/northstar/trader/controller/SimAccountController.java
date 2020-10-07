package tech.xuanwu.northstar.trader.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import tech.xuanwu.northstar.common.ResultBean;
import tech.xuanwu.northstar.service.ISimAccountService;

@Api(tags = "模拟账户接口")
@RequestMapping("/sim")
@RestController
public class SimAccountController {

	@Autowired
	ISimAccountService service;
	
	@ApiOperation("模拟入金")
	@PostMapping("/amount")
	ResultBean<Void> deposit(String gatewayId, int money){
		Assert.hasText(gatewayId, "模拟网关ID不能为空");
		Assert.isTrue(money>0, "入金金额为正整数");
		service.deposit(gatewayId, money);
		return new ResultBean<Void>(null);
	}
	
	@ApiOperation("模拟出金")
	@GetMapping("/amount")
	ResultBean<Void> withdraw(String gatewayId, int money){
		Assert.hasText(gatewayId, "模拟网关ID不能为空");
		Assert.isTrue(money>0, "出金金额为正整数");
		service.withdraw(gatewayId, money);
		return new ResultBean<Void>(null);
	}
}
