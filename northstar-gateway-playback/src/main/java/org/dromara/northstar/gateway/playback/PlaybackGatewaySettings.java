package org.dromara.northstar.gateway.playback;

import java.util.List;

import org.dromara.northstar.common.constant.FieldType;
import org.dromara.northstar.common.constant.PlaybackPrecision;
import org.dromara.northstar.common.constant.PlaybackSpeed;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.GatewaySettings;
import org.dromara.northstar.common.model.Setting;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlaybackGatewaySettings extends DynamicParams implements GatewaySettings {
	
	/**
	 * 预热起始时间
	 * 格式：YYYYMMDD
	 */
	@Setting(label="预热起始日", order=0, type=FieldType.DATE)
	private String preStartDate;
	/**
	 * 开始时间
	 * 格式：YYYYMMDD
	 */
	@Setting(label="开始时间", order=10, type=FieldType.DATE)
	private String startDate;
	/**
	 * 结束时间
	 * 格式：YYYYMMDD
	 */
	@Setting(label="结束时间", order=20, type=FieldType.DATE)
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
	@Setting(label="回放速度", order=40, type=FieldType.SELECT, options = {"正常", "快速", "超速"}, optionsVal = {"NORMAL", "SPRINT", "RUSH"})
	private PlaybackSpeed speed;
	/**
	 * 回放的合约清单
	 */
	private List<ContractSimpleInfo> playContracts;	// 该配置项不需要页面设置，默认与网关订阅合约保持一致
	
}
