package tech.xuanwu.northstar.trader.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import tech.xuanwu.northstar.common.ResultBean;
import tech.xuanwu.northstar.service.ITradeService;
import tech.xuanwu.northstar.trader.constants.Errors;
import tech.xuanwu.northstar.trader.domain.event.GatewayBroadcaster;
import tech.xuanwu.northstar.trader.model.vo.GatewayVO;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;

/**
 * 交易接口
 * @author kevinhuangwl
 *
 */
@Api(tags = "交易接口")
@RestController
public class TradeController {
	
	@Autowired
	ITradeService service;
	
	@Autowired
	GatewayBroadcaster gatewayBroadcaster;
	
	@Autowired
	ApplicationContext ctx;

	@ApiOperation("委托下单")
	@PostMapping("/submit")
	ResultBean<String> submitOrder(String gatewayId, String symbol, double price, int volume, DirectionEnum dir, 
			OffsetFlagEnum dealType, OrderPriceTypeEnum priceType, TimeConditionEnum timeCondition) {
		Assert.hasText(gatewayId, Errors.EMPTY_GATEWAYID);
		Assert.hasText(symbol, "合约名称不能为空");
		Assert.isTrue(price > 0, "价格必须为正数");
		Assert.isTrue(volume > 0, "交易手数必须为正数");
		Assert.notNull(dir, "交易方向不能为空");
		Assert.notNull(dealType, "开平类型不能为空");
		return new ResultBean<>(service.submitOrder(gatewayId, symbol, price, volume, dir, dealType, priceType, timeCondition));
	}
	
	@ApiOperation("撤单")
	@PostMapping("/cancel")
	ResultBean<Boolean> cancelOrder(String gatewayId, String orderId) {
		Assert.hasText(gatewayId, Errors.EMPTY_GATEWAYID);
		Assert.hasText(orderId, "订单ID不能为空");
		return new ResultBean<>(service.cancelOrder(gatewayId, orderId));
	}
	
	@ApiOperation("可交易账户列表")
	@GetMapping("/accountList")
	ResultBean<List<GatewayVO>> getTradableAccountList(){
		return new ResultBean<>(service.getTradableAccountList().stream().map(i -> GatewayVO.convertFrom(i)).collect(Collectors.toList()));
	}
	
	@ApiOperation("行情状态")
	@GetMapping("/dataRunState")
	ResultBean<Boolean> isGatewayDataOnRunning(){
		return new ResultBean<>(gatewayBroadcaster.isMarkDataOnRunning());
	}
	
	@ApiOperation("合约列表（Base64加密）")
	@GetMapping("/contracts")
	ResultBean<List<byte[]>> getContracts(){
		return new ResultBean(service.getContracts());
	}
}
