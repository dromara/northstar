package tech.xuanwu.northstar.strategy.common.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import tech.xuanwu.northstar.strategy.common.constants.ModuleState;

/**
 * 模组状态记录
 * @author KevinHuangwl
 *
 */
@Document
@Data
public class ModuleStatus {
	/**
	 * 
	 */
	private ModuleState state = ModuleState.EMPTY;
	
	private List<byte[]> lastOpenTrade = new ArrayList<>();
	
	private String moduleName;
}
