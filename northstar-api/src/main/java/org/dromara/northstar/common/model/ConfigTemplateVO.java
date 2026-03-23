package org.dromara.northstar.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 配置模板视图对象
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ConfigTemplateVO {

	/**
	 * 模板ID
	 */
	private String id;
	
	/**
	 * 模板名称
	 */
	private String name;
	
	/**
	 * 模板说明
	 */
	private String description;
	
	/**
	 * 适用交易所
	 */
	private String exchange;
	
	/**
	 * 适用产品类型
	 */
	private String productClass;
	
	/**
	 * 配置内容
	 */
	private ContractConfigDTO config;
}
