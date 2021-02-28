package tech.xuanwu.northstar.trader.domain.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.gateway.GatewayApi;
import tech.xuanwu.northstar.trader.constants.Constants;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;

@Slf4j
@Component
public class SMSInstructionStrategy implements InitializingBean{

	@Autowired
	@Qualifier(Constants.CONTRACT_MAP)
	private Map<String, ContractField> contractMap;
	
	private List<GatewayApi> bindedGateway = new ArrayList<>();
	
	private WinnerSMSResolver resolver;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		resolver = new WinnerSMSResolver(contractMap);
	}
	
	public void react(String msg) {
		SubmitOrderReqField[] orderReqs = resolver.resolve(msg);
		for(SubmitOrderReqField submitOrder : orderReqs) {
			for(GatewayApi gateway : bindedGateway) {
				gateway.submitOrder(submitOrder);
			}
			// 确保先平仓再开仓
			// TODO 可以优化成Promise
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				log.warn("中断休眠", e);
			}
		}
	}

	public void bind(GatewayApi gateway) {
		bindedGateway.add(gateway);
	}
	
}
