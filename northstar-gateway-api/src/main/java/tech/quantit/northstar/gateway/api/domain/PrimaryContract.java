package tech.quantit.northstar.gateway.api.domain;

import tech.quantit.northstar.common.constant.Constants;
import xyz.redtorch.pb.CoreField.ContractField;

/**
 * 主力合约
 * @author KevinHuangwl
 *
 */
public class PrimaryContract extends NormalContract {

	public PrimaryContract(ContractField indexContract) {
		super.field = ContractField.newBuilder(indexContract)
				.setSymbol(indexContract.getSymbol().replace(Constants.INDEX_SUFFIX, Constants.PRIMARY_SUFFIX))
				.setThirdPartyId(indexContract.getThirdPartyId().replace(Constants.INDEX_SUFFIX, Constants.PRIMARY_SUFFIX))
				.setContractId(indexContract.getContractId().replace(Constants.INDEX_SUFFIX, Constants.PRIMARY_SUFFIX))
				.setLastTradeDateOrContractMonth("")
				.setUnifiedSymbol(indexContract.getUnifiedSymbol().replace(Constants.INDEX_SUFFIX, Constants.PRIMARY_SUFFIX))
				.setFullName(indexContract.getFullName().replace("指数", "主力"))
				.setLongMarginRatio(0.1)
				.setShortMarginRatio(0.1)
				.setName(indexContract.getName().replace("指数", "主力"))
				.build();
		super.gatewayType = indexContract.getThirdPartyId().split("@")[1];
		super.updateTime = System.currentTimeMillis();
	}
}
