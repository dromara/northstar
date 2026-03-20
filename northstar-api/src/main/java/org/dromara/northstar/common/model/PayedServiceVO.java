package org.dromara.northstar.common.model;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PayedServiceVO {

	private String name;
	
	private LocalDate expiredDate;
}
