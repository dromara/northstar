package tech.xuanwu.northstar.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tech.xuanwu.northstar.common.exception.InsufficientException;
import tech.xuanwu.northstar.common.exception.TradeException;
import tech.xuanwu.northstar.common.model.OrderRecall;
import tech.xuanwu.northstar.common.model.OrderRequest;
import tech.xuanwu.northstar.service.AccountService;

/**
 * 交易控制器
 * @author KevinHuangwl
 *
 */
@RestController
@RequestMapping("/trade")
public class TradeOperationController {
	
	@Autowired
	protected AccountService accountService;

	@PostMapping("/submit")
	public boolean submitOrder(@RequestBody OrderRequest req) throws InsufficientException {
		Assert.hasText(req.getGatewayId(), "账户网关ID不能为空");
		Assert.hasText(req.getContractSymbol(), "合约不能为空");
		Assert.hasText(req.getPrice(), "价格不能为空");
		Assert.isTrue(req.getVolume() > 0, "下单手数必须为正整数");
		Assert.notNull(req.getTradeOpr(), "交易操作不能为空");
		return accountService.submitOrder(req);
	}
	
	@PostMapping("/cancel")
	public boolean cancelOrder(@RequestBody OrderRecall recall) throws TradeException {
		return accountService.cancelOrder(recall);
	}
	
}
