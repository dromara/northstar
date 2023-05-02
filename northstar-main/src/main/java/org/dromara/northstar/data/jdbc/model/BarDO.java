package org.dromara.northstar.data.jdbc.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@Entity
public class BarDO {
	
	@Id
	@GeneratedValue
	private int id;

	private String unifiedSymbol;
	
	private String tradingDay;
	
	private long expiredAt;
	
	private byte[] barData;

	public BarDO(String unifiedSymbol, String tradingDay, long expiredAt, byte[] barData) {
		super();
		this.unifiedSymbol = unifiedSymbol;
		this.tradingDay = tradingDay;
		this.expiredAt = expiredAt;
		this.barData = barData;
	}
	
}
