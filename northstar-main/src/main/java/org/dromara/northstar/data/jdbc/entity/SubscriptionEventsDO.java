package org.dromara.northstar.data.jdbc.entity;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@Entity
public class SubscriptionEventsDO {
	
	public static final String FIXED_ID = "SUB_EVENT";

	@Id
	private String id = FIXED_ID;
	
	private String subEvents;
}
