package tech.quantit.northstar.gateway.sim.persistence;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document
public class SimAccountPO {

	@Id
	private String gatewayId;
	
	private byte[] accountData;
	
	private List<byte[]> positionData;
}
