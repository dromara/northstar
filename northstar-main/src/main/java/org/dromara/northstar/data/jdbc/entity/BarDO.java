package org.dromara.northstar.data.jdbc.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import org.dromara.northstar.common.constant.DateTimeConstant;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreField.BarField;

@Slf4j
@NoArgsConstructor
@Data
@Entity
@Table(name="BAR", indexes = {
		@Index(name="idx_unifiedSymbol", columnList = "unifiedSymbol"),
		@Index(name="idx_tradingDay", columnList = "tradingDay")
})
public class BarDO {
	
	@Id
	@GeneratedValue
	private int id;

	private String unifiedSymbol;
	
	private String tradingDay;
	
	private long expiredAt;
	@Column(length = 1024)
	private byte[] barData;

	public BarDO(String unifiedSymbol, String tradingDay, long expiredAt, byte[] barData) {
		this.unifiedSymbol = unifiedSymbol;
		this.tradingDay = tradingDay;
		this.expiredAt = expiredAt;
		this.barData = barData;
	}
	
	public static BarDO convertFrom(BarField bar) {
		long expiredAt = LocalDateTime.of(LocalDate.parse(bar.getTradingDay(), DateTimeConstant.D_FORMAT_INT_FORMATTER), LocalTime.of(20, 0)).toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
		return new BarDO(bar.getUnifiedSymbol(), bar.getTradingDay(), expiredAt, bar.toByteArray()); 
	}
	
	public BarField convertTo() {
		try {
			return BarField.parseFrom(barData);
		} catch (Exception e) {
			log.warn("", e);
			return null;
		}
	}
	
}
