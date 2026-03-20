package org.dromara.northstar.app.restful;

import java.util.List;

import org.dromara.northstar.app.service.GatewayService;
import org.dromara.northstar.common.exception.ForbiddenInDemoException;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.GatewayVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("northstar/gateway")
@RestController
public class GatewayController {
	
	@Autowired
	GatewayService service;
	
	@Autowired
	private Environment env;
	
	/**
	 * 新增网关
	 * @param gd
	 * @return
	 */
	@PostMapping("create")
	public GatewayVO createGateway(@RequestBody GatewayDescription gd){
		if(env != null && env.matchesProfiles("demo")) {
			throw new ForbiddenInDemoException();
		}
		return service.createGateway(gd);
	}
	
	/**
	 * 修改网关
	 * @param gd
	 * @return
	 */
	@PutMapping("update")
	public GatewayVO updateGateway(@RequestBody GatewayDescription gd){
		if(env != null && env.matchesProfiles("demo")) {
			throw new ForbiddenInDemoException();
		}
		return service.updateGateway(gd);
	}
	
	/**
	 * 移除网关
	 * @param gatewayId
	 * @return
	 */
	@DeleteMapping("remove/{gatewayId}")
	public void removeGateway(@PathVariable String gatewayId){
		if(env != null && env.matchesProfiles("demo")) {
			throw new ForbiddenInDemoException();
		}
		service.removeGateway(gatewayId);
	}
	
	/**
	 * 查询全部网关
	 * @return
	 */
	@GetMapping("all")
	public List<GatewayVO> findAllGateway(){
		return service.findAllGateway();
	}
	
	/**
	 * 连线网关
	 * @param gatewayId
	 * @return
	 */
	@GetMapping("connect/{gatewayId}")
	public void connect(@PathVariable String gatewayId){
		service.connect(gatewayId);
	}
	
	/**
	 * 断开网关
	 * @param gatewayId
	 * @return
	 */
	@GetMapping("disconnect/{gatewayId}")
	public void disconnect(@PathVariable String gatewayId){
		service.disconnect(gatewayId);
	}
	
	/**
	 * 出入金
	 * @param gatewayId
	 * @param amount
	 * @return
	 */
	@PutMapping("deposit/{gatewayId}")
	public Integer moneyIO(@PathVariable String gatewayId, int amount){
		return service.moneyIO(gatewayId, amount);
	}
}
