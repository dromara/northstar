package org.dromara.northstar.data.jdbc.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import org.dromara.northstar.common.model.DealRecord;

import com.alibaba.fastjson2.JSON;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@Entity
@Table(name="DEALS", indexes = {
		@Index(name="idx_deal_account_name", columnList = "accountName"),
})
public class DealRecordDO {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	private int id;
	
	private String accountName;
	
	private long createTime;
	
	@Lob
	private String dataStr;
	
	public DealRecordDO(String accountName, String dataStr) {
		this.accountName = accountName;
		this.createTime = System.currentTimeMillis();
		this.dataStr = dataStr;
	}
	
	public static DealRecordDO convertFrom(DealRecord dealRecord) {
		return new DealRecordDO(dealRecord.getAccountName(), JSON.toJSONString(dealRecord));
	}

	public DealRecord convertTo() {
		return JSON.parseObject(dataStr, DealRecord.class);
	}
	
}
