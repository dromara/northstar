package tech.xuanwu.northstar.strategy.common.model.entity;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.xuanwu.northstar.strategy.common.constants.ModuleState;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Document
public class ModuleStatusEntity {

	@Id
	private String moduleName;
	
	private List<ModulePositionEntity> positions;
	
	private ModuleState state;
}
