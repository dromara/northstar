package tech.quantit.northstar.main.restful;

import java.util.List;

import javax.validation.constraints.NotNull;

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

import tech.quantit.northstar.common.constant.GatewayType;
import tech.quantit.northstar.common.constant.GatewayUsage;
import tech.quantit.northstar.common.model.ContractDefinition;
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
	@NotNull(message="网关ID不能为空")
	public ResultBean<Boolean> remove(String gatewayId) {
		return new ResultBean<>(gatewayService.deleteGateway(gatewayId));
	}
	
	@PutMapping
	public ResultBean<Boolean> modify(@RequestBody GatewayDescription gd) throws Exception {
		Assert.notNull(gd, "传入对象不能为空");
		return new ResultBean<>(gatewayService.updateGateway(gd));
	}
	
	@GetMapping
	@NotNull(message="网关用途不能为空")
	public ResultBean<List<GatewayDescription>> list(String usage) { 
		if(StringUtils.isBlank(usage)) {
			return new ResultBean<>(gatewayService.findAllGatewayDescription());
		}
		if(GatewayUsage.valueOf(usage) == GatewayUsage.MARKET_DATA) {
			return new ResultBean<>(gatewayService.findAllMarketGatewayDescription());
		}
		return new ResultBean<>(gatewayService.findAllTraderGatewayDescription());
	}
	
	@GetMapping("/specific")
	@NotNull(message="网关ID不能为空")
	public ResultBean<GatewayDescription> findGatewayDescription(String gatewayId){
		return new ResultBean<>(gatewayService.findGatewayDescription(gatewayId));
	}
	
	@GetMapping("/active")
	@NotNull(message="网关ID不能为空")
	public ResultBean<Boolean> getGatewayActive(String gatewayId){
		return new ResultBean<>(gatewayService.isActive(gatewayId));
	}
	
	@GetMapping("/connection")
	@NotNull(message="网关ID不能为空")
	public ResultBean<Boolean> connect(String gatewayId) {
		return new ResultBean<>(gatewayService.connect(gatewayId));
	}
	
	@DeleteMapping("/connection")
	@NotNull(message="网关ID不能为空")
	public ResultBean<Boolean> disconnect(String gatewayId) {
		return new ResultBean<>(gatewayService.disconnect(gatewayId));
	}
	
	@PostMapping("/moneyio")
	public ResultBean<Boolean> simMoneyIO(String gatewayId, int money){
		Assert.notNull(gatewayId, "网关ID不能为空");
		return new ResultBean<>(gatewayService.simMoneyIO(gatewayId, money));
	}
	
	@GetMapping("/contractDefs")
	@NotNull(message="网关类型不能为空")
	public ResultBean<List<ContractDefinition>> getContractDefinitions(GatewayType gatewayType){
		return new ResultBean<>(gatewayService.contractDefinitions(gatewayType));
	}
	
	@GetMapping("/subContracts")
	@NotNull(message="网关类型不能为空")
	public ResultBean<List<byte[]>> getSubscribedContracts(GatewayType gatewayType){
		return new ResultBean<>(gatewayService.getSubscribedContracts(gatewayType));
	}
}
