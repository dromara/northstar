package org.dromara.northstar.common.model.rl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RL 策略视图对象（仅包含实现 RLStrategy 接口的策略）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RLStrategyVO {

	private String name;
	private String className;
}
