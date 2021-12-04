package tech.quantit.northstar.main.restful;

import java.time.LocalDate;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.hutool.core.lang.Assert;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.common.model.ResultBean;
import tech.quantit.northstar.common.model.SimpleContractInfo;
import tech.quantit.northstar.main.service.DataSyncService;

@RequestMapping("/data")
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
	public ResultBean<Void> historyBars(String gatewayId, String unifiedSymbol, String startDate, String endDate) throws Exception {
		Assert.notBlank(gatewayId);
		Assert.notBlank(unifiedSymbol);
		if(StringUtils.isEmpty(startDate)) {
			startDate = LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER);
		}
		if(StringUtils.isEmpty(endDate)) {
			endDate = LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER);
		}
		LocalDate dateStart = LocalDate.parse(startDate, DateTimeConstant.D_FORMAT_INT_FORMATTER);
		LocalDate dateEnd = LocalDate.parse(endDate, DateTimeConstant.D_FORMAT_INT_FORMATTER);
		service.asyncLoadHistoryBarData(gatewayId, unifiedSymbol, dateStart, dateEnd);
		return new ResultBean<>(null);
	}
	
	@GetMapping("/contracts")
	public ResultBean<List<SimpleContractInfo>> availableContracts(){
		return new ResultBean<List<SimpleContractInfo>>(service.getAvailableContracts());
	}
}
