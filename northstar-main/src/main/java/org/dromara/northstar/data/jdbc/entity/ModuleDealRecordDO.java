package org.dromara.northstar.data.jdbc.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.dromara.northstar.common.model.ModuleDealRecord;

import com.alibaba.fastjson2.JSON;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@Entity
@Table(name="MODULE_DEAL", indexes = {
		@Index(name="idx_moduleName", columnList = "moduleName"),
		@Index(name="idx_createTime", columnList = "createTime"),
})
public class ModuleDealRecordDO {

	@Id
	@GeneratedValue
	private int id;
	
	private String moduleName;
	
	private long createTime;
	
	@Lob
	private String dataStr;
	
	public ModuleDealRecordDO(String moduleName, String dataStr) {
		this.moduleName = moduleName;
		this.createTime = System.currentTimeMillis();
		this.dataStr = dataStr;
	}
	
	public static ModuleDealRecordDO convertFrom(ModuleDealRecord dealRecord) {
		return new ModuleDealRecordDO(dealRecord.getModuleName(), JSON.toJSONString(dealRecord));
	}

	public ModuleDealRecord convertTo() {
		return JSON.parseObject(dataStr, ModuleDealRecord.class);
	}
	
}
