package tech.quantit.northstar.main;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.data.ds.DataServiceManager;
import tech.quantit.northstar.gateway.api.domain.ContractFactory;
import tech.quantit.northstar.gateway.api.domain.GlobalMarketRegistry;
import tech.quantit.northstar.gateway.api.domain.NormalContract;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreField.ContractField;

@Slf4j
@Component
@ConditionalOnExpression("!'${spring.profiles.active}'.equals('test')")
public class ContractsDefaultLoader implements CommandLineRunner{

	@Autowired
	private GlobalMarketRegistry registry;
	
	@Autowired
	private DataServiceManager dsMgr;
	
	@Override
	public void run(String... args) throws Exception {
		final LocalDate today = LocalDate.now();
		Map<String, ContractField> contractMap = new HashMap<>();
		List.of(ExchangeEnum.CFFEX, ExchangeEnum.SHFE, ExchangeEnum.DCE, ExchangeEnum.CZCE, ExchangeEnum.INE)
			.stream()
			.forEach(exchange -> {
				log.info("预加载 [{}] 交易所合约信息", exchange);
				dsMgr.getAllContracts(exchange).stream()
					//过滤掉过期合约
					.filter(contract -> LocalDate.parse(contract.getLastTradeDateOrContractMonth(), DateTimeConstant.D_FORMAT_INT_FORMATTER).isAfter(today))
					.forEach(contract -> {
						registry.register(new NormalContract(contract, 1));
						contractMap.put(contract.getUnifiedSymbol(), contract);
					});
			});
		// 构建指数合约
		ContractFactory contractFactory = new ContractFactory(contractMap.values().stream().toList());
		contractFactory.makeIndexContract().stream().forEach(registry::register);
	}

}
