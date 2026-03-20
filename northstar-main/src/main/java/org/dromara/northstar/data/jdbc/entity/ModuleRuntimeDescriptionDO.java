package org.dromara.northstar.data.jdbc.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

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
