package tech.xuanwu.northstar.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tech.xuanwu.northstar.common.constant.GatewayUsage;
import tech.xuanwu.northstar.common.model.GatewayDescription;
import tech.xuanwu.northstar.service.GatewayService;

@RequestMapping("/mgt")
@RestController
public class GatewayManagementController {
	
	@Autowired
	protected GatewayService gatewayService;

	@PostMapping("/gateway")
	public boolean create(@RequestBody GatewayDescription gd) {
		Assert.notNull(gd, "传入对象不能为空");
		return gatewayService.createGateway(gd);
	}
	
	@DeleteMapping("/gateway")
	public boolean remove(String gatewayId) {
		Assert.notNull(gatewayId, "网关ID不能为空");
		return gatewayService.deleteGateway(gatewayId);
	}
	
	@PutMapping("/gateway")
	public boolean modify(@RequestBody GatewayDescription gd) {
		Assert.notNull(gd, "传入对象不能为空");
		return gatewayService.updateGateway(gd);
	}
	
	@GetMapping("/gateway")
	public List<GatewayDescription> list(GatewayUsage usage) { 
		if(usage == GatewayUsage.MARKET_DATA) {
			return gatewayService.findAllMarketGateway();
		} else if (usage == GatewayUsage.TRADE) {
			return gatewayService.findAllTraderGateway();
		}
		return gatewayService.findAllGateway();
	}
	
	@GetMapping("/connection")
	public boolean connect(String gatewayId) {
		Assert.notNull(gatewayId, "网关ID不能为空");
		return gatewayService.connect(gatewayId);
	}
	
	@DeleteMapping("/connection")
	public boolean disconnect(String gatewayId) {
		Assert.notNull(gatewayId, "网关ID不能为空");
		return gatewayService.disconnect(gatewayId);
	}
}
