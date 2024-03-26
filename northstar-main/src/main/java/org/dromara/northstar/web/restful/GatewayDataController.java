package org.dromara.northstar.web.restful;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.ResultBean;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.data.IGatewayRepository;
import org.dromara.northstar.data.IMarketDataRepository;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.IContractManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import xyz.redtorch.pb.CoreField.BarField;

@RequestMapping("/northstar/data")
@RestController
public class GatewayDataController {

	@Autowired
	private IMarketDataRepository mdRepo;
	
	@Autowired
	private IGatewayRepository gatewayRepo;
	
	@Autowired
	private IContractManager contractMgr;
	
	@GetMapping("/bar/min")
	public ResultBean<List<byte[]>> loadWeeklyBarData(String gatewayId, String unifiedSymbol, long refStartTimestamp, boolean firstLoad){
		Assert.notNull(unifiedSymbol, "合约代码不能为空");
		GatewayDescription gd = gatewayRepo.findById(gatewayId);
		if(gd.getChannelType() == ChannelType.PLAYBACK || gd.getChannelType() == ChannelType.SIM) {
			return new ResultBean<>(Collections.emptyList());
		}
		IContract ic = contractMgr.getContract(gd.getChannelType(), unifiedSymbol);
		IDataSource ds = ic.dataSource();
		LocalDate refDate = CommonUtils.millsToLocalDateTime(refStartTimestamp).toLocalDate();
		List<Bar> dsData = ds.getMinutelyData(ic.contract(), refDate.minusDays(refDate.getDayOfWeek().getValue() - 1L), refDate);
		LocalDate queryStart = refDate;
		if(!dsData.isEmpty()) {
			queryStart = dsData.get(0).tradingDay().plusDays(1);
		}
		List<Bar> localData = new ArrayList<>();
		if(firstLoad) {
			localData.addAll(mdRepo.loadBars(ic, queryStart, queryStart.plusDays(3)));
		}
		
		List<Bar> result = new ArrayList<>();
		result.addAll(dsData.reversed());
		result.addAll(localData);
		return new ResultBean<>(result.stream()
				.map(Bar::toBarField)
				.map(BarField::toByteArray)
				.toList());
	}
	
}
