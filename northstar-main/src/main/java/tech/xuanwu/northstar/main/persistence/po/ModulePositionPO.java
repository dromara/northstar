package tech.xuanwu.northstar.main.persistence.po;

import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.xuanwu.northstar.strategy.api.model.ModulePositionInfo;

@Document
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModulePositionPO {
	
	private String moduleName;

	private List<ModulePositionInfo> positions;
}
