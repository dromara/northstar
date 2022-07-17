package tech.quantit.northstar.common.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class LogDescription {

	private long startPosition;
	
	private long endPosition;
	
	private List<String> linesOfLog;
}
