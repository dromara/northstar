package tech.xuanwu.northstar.common.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.xuanwu.northstar.common.constant.PlaybackPrecision;

/**
 * 回测请求描述
 * @author KevinHuangwl
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaybackDescription {
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
	 * 回测精度
	 */
	private PlaybackPrecision precision;
	/**
	 * 回测模组列表
	 */
	private List<String> moduleNames;
	/**
	 * 回测账户初始金额
	 */
	private int playbackAccountInitialBalance;
	/**
	 * 回测交易手续费（以元为单位）
	 */
	private int fee;
}
