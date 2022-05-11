package tech.quantit.northstar.data.mongo.po;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import tech.quantit.northstar.common.model.ModuleDescription;

/**
 * 模组配置信息
 * @author KevinHuangwl
 *
 */
@Data
@Document
public class ModuleSettingsDescriptionPO {
	@Id
	private String moduleName;
	
	private ModuleDescription moduleSettingsDescription;
	
	public static ModuleSettingsDescriptionPO convertFrom(ModuleDescription moduleSettingsDescription) {
		ModuleSettingsDescriptionPO po = new ModuleSettingsDescriptionPO();
		po.moduleName = moduleSettingsDescription.getModuleName();
		po.moduleSettingsDescription = moduleSettingsDescription;
		return po;
	}
}
