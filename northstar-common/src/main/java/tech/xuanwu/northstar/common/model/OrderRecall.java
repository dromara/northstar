package tech.xuanwu.northstar.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 撤单委托
 * @author KevinHuangwl
 *
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class OrderRecall {
	
	private String originOrderId;
	
	private String gatewayId;

}
