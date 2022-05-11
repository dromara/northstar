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
public class ModuleDescriptionPO {

	@Id
	private String moduleName;
	
	private ModuleRuntimeDescription moduleDescription;
	
	public static ModuleDescriptionPO convertFrom(ModuleRuntimeDescription moduleDescription) {
		ModuleDescriptionPO po = new ModuleDescriptionPO();
		po.moduleName = moduleDescription.getModuleName();
		po.moduleDescription = moduleDescription;
		return po;
	}
}
