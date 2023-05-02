package org.dromara.northstar.data.jdbc.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.dromara.northstar.common.model.ModuleDescription;

import com.alibaba.fastjson2.JSON;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name="MODULE_DESCRIPTION", indexes = {
		@Index(name="idx_moduleName", columnList = "moduleName"),
})
public class ModuleDescriptionDO {

	@Id
	private String moduleName;
	@Lob
	private String dataStr;
	
	public static ModuleDescriptionDO convertFrom(ModuleDescription moduleDescription) {
		return new ModuleDescriptionDO(moduleDescription.getModuleName(), JSON.toJSONString(moduleDescription));
	}
	
	public ModuleDescription convertTo() {
		return JSON.parseObject(dataStr, ModuleDescription.class);
	}
}
