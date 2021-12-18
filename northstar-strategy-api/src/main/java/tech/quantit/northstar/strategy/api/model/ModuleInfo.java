package tech.quantit.northstar.strategy.api.model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.quantit.northstar.strategy.api.constant.ModuleType;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document
@Data
public class ModuleInfo {
	/**
	 * 信号策略
	 */
	private ComponentAndParamsPair signalPolicy;
	/**
	 * 风控规则
	 */
	private List<ComponentAndParamsPair> riskControlRules;
	/**
	 * 交易策略
	 */
	private ComponentAndParamsPair dealer;
	/**
	 * 账户ID
	 */
	private String accountGatewayId;
	/**
	 * 模组名称
	 */
	@Id
	private String moduleName;
	/**
	 * 是否启用
	 */
	private boolean enabled;
	/**
	 * 策略类型
	 */
	private ModuleType type;
	/**
	 * 历史数据加载长度（单位：天）
	 */
	private int numOfDaysOfDataRef;
}
