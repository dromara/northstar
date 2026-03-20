package org.dromara.northstar.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExplorerTaskVO {

	/**
	 * 任务描述
	 */
	private ExplorerTaskDescription description;
	
	/**
	 * 执行进度
	 */
	private int progress;
	
}
