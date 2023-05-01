package org.dromara.northstar.data.jdbc.model;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
public class GatewayDescriptionDO {

	@Id
	private String gatewayId;
	
	private String dataStr;
}
