package tech.quantit.northstar.data.mongo.po;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import tech.quantit.northstar.common.model.ModuleRuntimeDescription;

/**
 * 模组状态信息
 * @author KevinHuangwl
 *
 */
@Data
@Document
public class ModuleRuntimeDescriptionPO {

	@Id
	private String moduleName;
	
	private ModuleRuntimeDescription moduleDescription;
	
	public static ModuleRuntimeDescriptionPO convertFrom(ModuleRuntimeDescription moduleDescription) {
		ModuleRuntimeDescriptionPO po = new ModuleRuntimeDescriptionPO();
		po.moduleName = moduleDescription.getModuleName();
		po.moduleDescription = moduleDescription;
		return po;
	}
}
