package org.dromara.northstar.data.jdbc.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.Table;

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
		@Index(name="idx_unifiedSymbol", columnList = "unifiedSymbol"),
		@Index(name="idx_tradingDay", columnList = "tradingDay")
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
