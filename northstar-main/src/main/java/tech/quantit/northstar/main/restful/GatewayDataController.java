package tech.quantit.northstar.main.restful;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tech.quantit.northstar.common.model.Identifier;
import tech.quantit.northstar.common.model.ResultBean;
import tech.quantit.northstar.common.utils.MarketDataLoadingUtils;
import tech.quantit.northstar.data.IMarketDataRepository;
import tech.quantit.northstar.gateway.api.IContractManager;
import tech.quantit.northstar.gateway.api.domain.contract.Contract;
import xyz.redtorch.pb.CoreField.BarField;

@RequestMapping("/northstar/data")
@RestController
public class GatewayDataController {

	@Autowired
	private IMarketDataRepository mdRepo;
	
	@Autowired
	private IContractManager contractMgr;
	
	private MarketDataLoadingUtils utils = new MarketDataLoadingUtils();
	
	@GetMapping("/bar/min")
	public ResultBean<List<byte[]>> loadWeeklyBarData(String contractId, long refStartTimestamp, boolean firstLoad){
		Assert.notNull(contractId, "合约代码不能为空");
		Contract contract = contractMgr.getContract(Identifier.of(contractId));
		LocalDate start = utils.getFridayOfLastWeek(refStartTimestamp);
		if(firstLoad && Period.between(start, LocalDate.now()).getDays() < 7) {
			start = start.minusWeeks(1);
		}
		LocalDate end = utils.getCurrentTradeDay(refStartTimestamp, firstLoad);
		return new ResultBean<>(
				mdRepo.loadBars(contract.channelType(), contract.contractField().getUnifiedSymbol(), start, end)
				.stream()
				.map(BarField::toByteArray)
				.toList());
	}
	
}
