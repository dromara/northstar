package org.dromara.northstar.gateway.playback;

import org.dromara.northstar.common.GatewaySettings;
import org.dromara.northstar.common.constant.FieldType;
import org.dromara.northstar.common.constant.PlaybackPrecision;
import org.dromara.northstar.common.constant.PlaybackSpeed;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.Setting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PlaybackGatewaySettings extends DynamicParams implements GatewaySettings {
	/**
	 * 预热起始时间
	 * 格式：YYYYMMDD
	 */
	@Setting(label="预热起始日", order=0, type=FieldType.DATE, required = false)
	private String preStartDate;
	/**
	 * 开始时间
	 * 格式：YYYYMMDD
	 */
	@Setting(label="开始日", order=10, type=FieldType.DATE)
	private String startDate;
	/**
	 * 结束时间
	 * 格式：YYYYMMDD
	 */
	@Setting(label="结束日", order=20, type=FieldType.DATE)
	private String endDate;
	/**
	 * 回放精度
	 */
	@Setting(label="回放精度", order=30, type=FieldType.SELECT, 
			options = {"低（每分钟4个TICK）", "中（每分钟30个TICK）", "高（每分钟120个TICK）"}, 
			optionsVal = {"LOW", "MEDIUM", "HIGH"})
	private PlaybackPrecision precision;
	/**
	 * 回放速度
	 */
	@Setting(label="回放速度", order=40, type=FieldType.SELECT, options = {"正常", "快速", "极速"}, optionsVal = {"NORMAL", "SPRINT", "RUSH"})
	private PlaybackSpeed speed;
	
}
