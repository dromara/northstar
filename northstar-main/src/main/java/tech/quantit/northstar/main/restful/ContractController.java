package tech.quantit.northstar.main.restful;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tech.quantit.northstar.common.model.ContractDefinition;
import tech.quantit.northstar.common.model.ResultBean;
import tech.quantit.northstar.main.service.ContractService;
import xyz.redtorch.pb.CoreField.ContractField;

@RequestMapping("/northstar/contract")
@RestController
public class ContractController {
	
	@Autowired
	private ContractService service;
	
	final CacheControl cacheControl = CacheControl
			.maxAge(1, TimeUnit.DAYS)
		    .noTransform()
		    .mustRevalidate();

	@GetMapping("/defs")
	@NotNull(message="合约类别名称不能为空")
	public ResponseEntity<ResultBean<List<ContractDefinition>>> getContractDefinitions(String provider){
		return ResponseEntity.ok()
			      .cacheControl(cacheControl)
			      .body(new ResultBean<>(service.getContractDefinitions(provider)));
	}
	
	@GetMapping("/providers")
	@NotNull(message="网关类型不能为空")
	public ResponseEntity<ResultBean<List<String>>> providerList(String gatewayType){
		return ResponseEntity.ok()
			      .cacheControl(cacheControl)
			      .body(new ResultBean<>(service.getContractProviders(gatewayType)));
	}
	
	@GetMapping("/list")
	@NotNull(message="合约类别名称不能为空")
	public ResultBean<List<byte[]>> getContractList(String provider){
		return new ResultBean<>(service.getContractList(provider)
				.stream()
				.map(ContractField::toByteArray)
				.toList());
	}
	
	@GetMapping("/subable")
	@NotNull(message="合约定义ID不能为空")
	public ResultBean<List<byte[]>> getSubscribableContractList(String contractDefId){
		return new ResultBean<>(service.getSubscribableContractList(contractDefId)
				.stream()
				.map(ContractField::toByteArray)
				.toList());
	}
}
