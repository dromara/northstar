package org.dromara.northstar.data.jdbc.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name="BAR", indexes = {
		@Index(name="idx_symbol_tradingDay", columnList = "unifiedSymbol, tradingDay"),
		@Index(name="idx_expiredAt", columnList = "expiredAt")
})
public class BarDO {
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	private int id;

	private String unifiedSymbol;
	
	private String tradingDay;
	
	private long expiredAt;
	@Lob
	private byte[] barData;

}
