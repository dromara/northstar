package tech.xuanwu.northstar.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tech.xuanwu.northstar.common.model.OrderRecall;
import tech.xuanwu.northstar.common.model.OrderRequest;
import tech.xuanwu.northstar.service.GatewayService;

/**
 * 交易控制器
 * @author KevinHuangwl
 *
 */
@RestController
@RequestMapping("/trade")
public class TradeOperationController {
	
	@Autowired
	protected GatewayService gatewayService;

	@PostMapping("/submit")
	public String submitOrder(@RequestBody OrderRequest req) {
		Assert.hasText(req.getAccountId(), "账户ID不能为空");
		Assert.hasText(req.getContractSymbol(), "合约不能为空");
		Assert.hasText(req.getPrice(), "价格不能为空");
		Assert.isTrue(req.getVolume() > 0, "下单手数必须为正整数");
		Assert.notNull(req.getTradeOpr(), "交易操作不能为空");
		return gatewayService.submitOrder(req);
	}
	
	@PostMapping("/cancel")
	public boolean cancelOrder(@RequestBody OrderRecall recall) {
		return gatewayService.cancelOrder(recall);
	}
	
}
