package org.dromara.northstar.common.model;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批量操作结果
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class BatchResult {

	/**
	 * 成功数量
	 */
	@Builder.Default
	private int success = 0;
	
	/**
	 * 失败数量
	 */
	@Builder.Default
	private int failed = 0;
	
	/**
	 * 错误信息列表
	 */
	@Builder.Default
	private List<String> errors = new ArrayList<>();
	
	/**
	 * 增加成功计数
	 */
	public void incrementSuccess() {
		this.success++;
	}
	
	/**
	 * 增加失败计数
	 */
	public void incrementFailed(String error) {
		this.failed++;
		if (error != null) {
			this.errors.add(error);
		}
	}
}
