package tech.quantit.northstar.main;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.ChannelType;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.data.ds.DataServiceManager;
import tech.quantit.northstar.gateway.api.IMarketCenter;
import tech.quantit.northstar.gateway.api.domain.contract.Instrument;
import tech.quantit.northstar.gateway.ctp.CtpContract;
import tech.quantit.northstar.gateway.sim.trade.SimContractGenerator;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;

@Slf4j
@Component
@ConditionalOnExpression("!'${spring.profiles.active}'.equals('test')")
public class ContractsDefaultLoader implements CommandLineRunner{

	@Autowired
	private IMarketCenter mktCenter;
	
	@Autowired
	private DataServiceManager dsMgr;
	
	@Override
	public void run(String... args) throws Exception {
		final LocalDate today = LocalDate.now();
		// 加载CTP合约
		List.of(ExchangeEnum.CFFEX, ExchangeEnum.SHFE, ExchangeEnum.DCE, ExchangeEnum.CZCE, ExchangeEnum.INE)
			.parallelStream()
			.forEach(exchange -> {
				dsMgr.getAllContracts(exchange).stream()
					//过滤掉过期合约
					.filter(contract -> LocalDate.parse(contract.getLastTradeDateOrContractMonth(), DateTimeConstant.D_FORMAT_INT_FORMATTER).isAfter(today))
					.forEach(contract -> mktCenter.addInstrument(new CtpContract(contract)));
				log.info("预加载 [{}] 交易所合约信息", exchange);
			});
		mktCenter.loadContractGroup(ChannelType.CTP);
		
		// 加载模拟合约
		SimContractGenerator contractGen = new SimContractGenerator("SIM");
		Instrument simContract = contractGen.getContract();
		mktCenter.addInstrument(simContract);
	}

}
