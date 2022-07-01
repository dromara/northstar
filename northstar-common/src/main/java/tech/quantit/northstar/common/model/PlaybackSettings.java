package tech.quantit.northstar.common.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.quantit.northstar.common.constant.PlaybackPrecision;
import tech.quantit.northstar.common.constant.PlaybackSpeed;

/**
 * 回测请求描述
 * @author KevinHuangwl
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaybackSettings implements GatewaySettings{
	/**
	 * 开始时间
	 * 格式：YYYYMMDD
	 */
	private String startDate;
	/**
	 * 结束时间
	 * 格式：YYYYMMDD
	 */
	private String endDate;
	/**
	 * 回放精度
	 */
	private PlaybackPrecision precision;
	/**
	 * 回放速度
	 */
	private PlaybackSpeed speed;
	/**
	 * 回放的合约清单
	 */
	List<String> unifiedSymbols;
}
