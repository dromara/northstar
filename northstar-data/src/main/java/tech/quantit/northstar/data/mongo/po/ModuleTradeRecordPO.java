package tech.quantit.northstar.data.mongo.po;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tech.quantit.northstar.common.model.ModuleTradeRecord;

/**
 * 模组成交记录
 * @author wpxs
 *
 */
@Data
@Document
public class ModuleTradeRecordPO {

	private String moduleName;

	private ModuleTradeRecord moduleTradeRecord;

	public static ModuleTradeRecordPO convertFrom(ModuleTradeRecord moduleTradeRecord) {
		ModuleTradeRecordPO po = new ModuleTradeRecordPO();
		po.moduleName = moduleTradeRecord.getModuleName();
		po.moduleTradeRecord = moduleTradeRecord;
		return po;
	}
}
