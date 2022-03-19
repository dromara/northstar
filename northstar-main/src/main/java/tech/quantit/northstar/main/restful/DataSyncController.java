package tech.quantit.northstar.main.restful;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.hutool.core.lang.Assert;
import tech.quantit.northstar.common.model.ResultBean;
import tech.quantit.northstar.common.model.SimpleContractInfo;
import tech.quantit.northstar.main.service.DataSyncService;

@RequestMapping("/northstar/data")
@RestController
public class DataSyncController {
	
	@Autowired
	private DataSyncService service;
	
	@GetMapping("/sync")
	public ResultBean<Void> sync() throws Exception {
		service.asyncUpdateContracts();
		service.asyncUpdateTradeAccount();
		return new ResultBean<>(null);
	}
	
	@GetMapping("/his/bar")
	public ResultBean<List<byte[]>> historyBars(String gatewayId, String unifiedSymbol, long startRefTime) throws Exception {
		Assert.notBlank(gatewayId);
		Assert.notBlank(unifiedSymbol);
		Assert.isTrue(startRefTime > 0);
		
		return new ResultBean<>(service.loadHistoryBarData(gatewayId, unifiedSymbol, startRefTime));
	}
	
	@GetMapping("/contracts")
	public ResultBean<List<SimpleContractInfo>> availableContracts(){
		return new ResultBean<>(service.getAvailableContracts());
	}
}
