package tech.quantit.northstar.data.mongo.po;

import java.time.LocalDate;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import xyz.redtorch.pb.CoreField.ContractField;

/**
 * 合约数据
 * @author KevinHuangwl
 *
 */
@Document
@Data
public class ContractPO {

	@Id
	private String unifiedSymbol;
	
	private String type;
	
	private int expiredDate;
	
	private byte[] data;

	public static ContractPO convertFrom(ContractField contract) {
		ContractPO po = new ContractPO();
		po.unifiedSymbol = contract.getUnifiedSymbol();
		String dateStr = contract.getLastTradeDateOrContractMonth();
		if(StringUtils.isEmpty(dateStr)) {
			dateStr = LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER);
		}
		po.type = contract.getProductClass().toString();
		po.expiredDate = Integer.parseInt(dateStr);
		po.data = contract.toByteArray();
		return po;
	}
}
