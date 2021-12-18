package tech.quantit.northstar.main.restful;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tech.quantit.northstar.common.exception.InsufficientException;
import tech.quantit.northstar.common.exception.TradeException;
import tech.quantit.northstar.common.model.OrderRecall;
import tech.quantit.northstar.common.model.OrderRequest;
import tech.quantit.northstar.common.model.ResultBean;
import tech.quantit.northstar.main.service.AccountService;
import tech.quantit.northstar.main.service.SMSTradeService;

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
	
	@Autowired
	protected SMSTradeService smsTradeService;

	@PostMapping("/submit")
	public ResultBean<Boolean> submitOrder(@RequestBody OrderRequest req) throws InsufficientException {
		Assert.hasText(req.getGatewayId(), "账户网关ID不能为空");
		Assert.hasText(req.getContractUnifiedSymbol(), "合约不能为空");
		Assert.hasText(req.getPrice(), "价格不能为空");
		Assert.isTrue(req.getVolume() > 0, "下单手数必须为正整数");
		Assert.notNull(req.getTradeOpr(), "交易操作不能为空");
		return new ResultBean<>(accountService.submitOrder(req));
	}
	
	@PostMapping("/cancel")
	public ResultBean<Boolean> cancelOrder(@RequestBody OrderRecall recall) throws TradeException {
		return new ResultBean<>(accountService.cancelOrder(recall));
	}
	
	@PostMapping(value="/sms", consumes = {"text/plain"})
	public ResultBean<Boolean> tradeBySMS(@RequestBody String text){
		smsTradeService.dispatchMsg(text);
		return new ResultBean<>(true);
	}
}
