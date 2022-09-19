package tech.quantit.northstar.gateway.playback;

import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;
import tech.quantit.northstar.common.constant.FieldType;
import tech.quantit.northstar.common.constant.PlaybackPrecision;
import tech.quantit.northstar.common.constant.PlaybackSpeed;
import tech.quantit.northstar.common.model.DynamicParams;
import tech.quantit.northstar.common.model.GatewaySettings;
import tech.quantit.northstar.common.model.Setting;
import tech.quantit.northstar.gateway.api.GatewaySettingsMetaInfoProvider;

@Getter
@Setter
@Component
public class PlaybackGatewaySettings extends DynamicParams implements GatewaySettings, InitializingBean{
	
	@Autowired
	private GatewaySettingsMetaInfoProvider pvd;
	
	@Autowired
	private PLAYBACK playback;
	
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
	@Setting(label="回放速度", order=40, type=FieldType.SELECT, options = {"正常", "极速"}, optionsVal = {"NORMAL", "SPRINT"})
	private PlaybackSpeed speed;
	/**
	 * 回放的合约清单
	 */
	@Setting(label="回放合约", order=50, type=FieldType.MULTI_SELECT)
	private List<String> unifiedSymbols;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		pvd.addSettings(playback.name(), this);
	}
}
