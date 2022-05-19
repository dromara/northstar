package tech.quantit.northstar.main.restful;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tech.quantit.northstar.common.constant.GatewayType;
import tech.quantit.northstar.common.model.ResultBean;
import tech.quantit.northstar.data.IContractRepository;
import xyz.redtorch.pb.CoreField.ContractField;

@RequestMapping("/northstar/data")
@RestController
public class GatewayDataController {

	@Autowired
	private IContractRepository repo;
	
	@GetMapping("/contracts")
	public ResultBean<List<byte[]>> getContracts(GatewayType type){
		return new ResultBean<>(repo.findAll(type).stream().map(ContractField::toByteArray).toList());
	}
	
	
}
