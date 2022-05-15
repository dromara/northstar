package tech.quantit.northstar.data.mongo.po;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import tech.quantit.northstar.common.model.ModuleDealRecord;

/**
 * 模组成交记录
 * @author wpxs
 *
 */
@Data
@Document
public class ModuleDealRecordPO {

	private String moduleName;

	private ModuleDealRecord moduleDealRecord;

	public static ModuleDealRecordPO convertFrom(ModuleDealRecord moduleDealRecord) {
		ModuleDealRecordPO po = new ModuleDealRecordPO();
		po.moduleName = moduleDealRecord.getModuleName();
		po.moduleDealRecord = moduleDealRecord;
		return po;
	}
}
