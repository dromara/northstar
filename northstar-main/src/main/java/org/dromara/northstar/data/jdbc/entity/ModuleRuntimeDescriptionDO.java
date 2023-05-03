package org.dromara.northstar.data.jdbc.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.dromara.northstar.common.model.ModuleRuntimeDescription;

import com.alibaba.fastjson2.JSON;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name="MODULE_RT", indexes = {
		@Index(name="idx_rt_moduleName", columnList = "moduleName"),
})
public class ModuleRuntimeDescriptionDO {

	@Id
	private String moduleName;
	@Lob
	private String dataStr;
	
	public static ModuleRuntimeDescriptionDO convertFrom(ModuleRuntimeDescription moduleRtDescription) {
		return new ModuleRuntimeDescriptionDO(moduleRtDescription.getModuleName(), JSON.toJSONString(moduleRtDescription));
	}
	
	public ModuleRuntimeDescription convertTo() {
		return JSON.parseObject(dataStr, ModuleRuntimeDescription.class);
	}
}
