package org.dromara.northstar.gateway.playback;

import java.time.LocalDate;

import org.dromara.northstar.common.GatewaySettings;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.gateway.Gateway;
import org.dromara.northstar.gateway.GatewayFactory;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.dromara.northstar.gateway.mktdata.NorthstarDataSource;
import org.springframework.util.Assert;

import com.alibaba.fastjson2.JSON;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PlaybackGatewayFactory implements GatewayFactory{

	private FastEventEngine feEngine;
	
	private MarketCenter mktCenter;
	
	public PlaybackGatewayFactory(FastEventEngine feEngine, MarketCenter mktCenter, NorthstarDataSource ds) {
		this.feEngine = feEngine;
		this.mktCenter = mktCenter;
		final LocalDate startDate = LocalDate.of(2020, 5, 1);	// 2020年5月1日前的数据会缺少持仓数据，故不建议使用
		try {
			log.info("加载回测合约");
			ds.getAllContracts().stream()
				.filter(contract -> contract.lastTradeDate().isAfter(startDate))
				.forEach(contract -> mktCenter.addInstrument(new PlaybackContract(contract, ds)));
			mktCenter.loadContractGroup(ChannelType.PLAYBACK);
		} catch (Exception e) {
			log.error("加载回测合约异常", e);
		}
	}
	
	@Override
	public Gateway newInstance(GatewayDescription gatewayDescription) {
		GatewayDescription gd = enhance(gatewayDescription);
		return new PlaybackGatewayAdapter(gd, feEngine, mktCenter);
	}

	@Override
	public GatewayDescription enhance(GatewayDescription gatewayDescription) {
		PlaybackGatewaySettings settings = JSON.parseObject(JSON.toJSONString(gatewayDescription.getSettings()), PlaybackGatewaySettings.class);
		Assert.hasText(settings.getStartDate(), "开始日不能为空");
		Assert.hasText(settings.getEndDate(), "结束日不能为空");
		Assert.notNull(settings.getPrecision(), "回放精度不能为空");
		Assert.notNull(settings.getSpeed(), "回放速度不能为空");
		LocalDate start = LocalDate.parse(settings.getStartDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER);
		LocalDate end = LocalDate.parse(settings.getEndDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER);
		Assert.isTrue(!start.isAfter(end), "开始日期不能晚于结束日期");
		gatewayDescription.setSettings(settings);
		return gatewayDescription;
	}

	@Override
	public GatewaySettings gatewaySettings() {
		return new PlaybackGatewaySettings();
	}

	@Override
	public ChannelType channelType() {
		return ChannelType.PLAYBACK;
	}
	
}
