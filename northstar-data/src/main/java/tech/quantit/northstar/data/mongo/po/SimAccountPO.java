package tech.quantit.northstar.data.mongo.po;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import tech.quantit.northstar.common.model.SimAccountDescription;

/**
 * 模拟账户状态信息
 * @author KevinHuangwl
 *
 */
@Data
@Document
public class SimAccountPO {
	
	@Id
	private String gatewayId;
	
	private SimAccountDescription simAccount;

	public static SimAccountPO convertFrom(SimAccountDescription simAccount) {
		SimAccountPO po = new SimAccountPO();
		po.gatewayId = simAccount.getGatewayId();
		po.simAccount = simAccount;
		return po; 
	}
}
