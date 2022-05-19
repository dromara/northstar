package tech.quantit.northstar.main.restful;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tech.quantit.northstar.common.constant.GatewayUsage;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.common.model.ResultBean;
import tech.quantit.northstar.domain.gateway.ContractManager;
import tech.quantit.northstar.main.service.GatewayService;

@RequestMapping("/northstar/gateway")
@RestController
public class GatewayManagementController {
	
	@Autowired
	protected GatewayService gatewayService;
	
	@Autowired
	protected ContractManager contractMgr;
	
	@PostMapping
	public ResultBean<Boolean> create(@RequestBody GatewayDescription gd) throws Exception {
		Assert.notNull(gd, "传入对象不能为空");
		return new ResultBean<>(gatewayService.createGateway(gd));
	}
	
	@DeleteMapping
	public ResultBean<Boolean> remove(String gatewayId) {
		Assert.notNull(gatewayId, "网关ID不能为空");
		return new ResultBean<>(gatewayService.deleteGateway(gatewayId));
	}
	
	@PutMapping
	public ResultBean<Boolean> modify(@RequestBody GatewayDescription gd) throws Exception {
		Assert.notNull(gd, "传入对象不能为空");
		return new ResultBean<>(gatewayService.updateGateway(gd));
	}
	
	@GetMapping
	public ResultBean<List<GatewayDescription>> list(String usage) throws Exception { 
		if(StringUtils.isBlank(usage)) {
			return new ResultBean<>(gatewayService.findAllGateway());
		}
		if(GatewayUsage.valueOf(usage) == GatewayUsage.MARKET_DATA) {
			return new ResultBean<>(gatewayService.findAllMarketGateway());
		}
		return new ResultBean<>(gatewayService.findAllTraderGateway());
	}
	
	@GetMapping("/active")
	public ResultBean<Boolean> getGatewayActive(String gatewayId){
		Assert.notNull(gatewayId, "网关ID不能为空");
		return new ResultBean<>(gatewayService.isActive(gatewayId));
	}
	
	@GetMapping("/connection")
	public ResultBean<Boolean> connect(String gatewayId) {
		Assert.notNull(gatewayId, "网关ID不能为空");
		return new ResultBean<>(gatewayService.connect(gatewayId));
	}
	
	@DeleteMapping("/connection")
	public ResultBean<Boolean> disconnect(String gatewayId) {
		Assert.notNull(gatewayId, "网关ID不能为空");
		return new ResultBean<>(gatewayService.disconnect(gatewayId));
	}
	
	@PostMapping("/moneyio")
	public ResultBean<Boolean> simMoneyIO(String gatewayId, int money){
		Assert.notNull(gatewayId, "网关ID不能为空");
		return new ResultBean<>(gatewayService.simMoneyIO(gatewayId, money));
	}
	
}
