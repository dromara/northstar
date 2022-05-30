package tech.quantit.northstar.main.restful;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tech.quantit.northstar.common.model.ResultBean;
import tech.quantit.northstar.data.IMarketDataRepository;
import xyz.redtorch.pb.CoreField.BarField;

@RequestMapping("/northstar/data")
@RestController
public class GatewayDataController {

	@Autowired
	private IMarketDataRepository mdRepo;
	
	@GetMapping("/bar/min")
	@NotNull
	public ResultBean<List<byte[]>> loadWeeklyBarData(String gatewayId, String unifiedSymbol, long refStartTimestamp){
		LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(refStartTimestamp), ZoneId.systemDefault());
		LocalDate end = ldt.toLocalDate();
		int dayOfWeekVal = end.getDayOfWeek().getValue();
		int diffDayOfFirstDayOfWeek = dayOfWeekVal - 1;
		LocalDate start = end.minusDays(diffDayOfFirstDayOfWeek);
		return new ResultBean<>(
				mdRepo
				.loadBars(gatewayId, unifiedSymbol, start, end)
				.stream()
				.map(BarField::toByteArray)
				.toList());
	}
	
	
}
