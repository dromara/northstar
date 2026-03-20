package org.dromara.northstar.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 自定义指标视图对象
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CustomIndicatorVO {

	/**
	 * 指标ID
	 */
	private String id;

	/**
	 * 指标简称（唯一标识）
	 */
	private String shortName;

	/**
	 * 指标描述
	 */
	private String description;

	/**
	 * 指标系列：price-主图，volume-副图
	 */
	private String series;

	/**
	 * 指标模板代码
	 */
	private String indicatorTemplateCode;

	/**
	 * 是否收藏
	 */
	private Boolean favorite;

	/**
	 * 创建时间戳
	 */
	private Long createdAt;

	/**
	 * 更新时间戳
	 */
	private Long updatedAt;
}
