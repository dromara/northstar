package tech.xuanwu.northstar.persistence.po;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Document
@Data
public class ContractPO {

	@Id
	private String contractId;

	private String unifiedSymbol;
	
	private String name;
	
	private String fullName;
	
	private String gatewayId;
	
	private long recordTimestamp;
}
