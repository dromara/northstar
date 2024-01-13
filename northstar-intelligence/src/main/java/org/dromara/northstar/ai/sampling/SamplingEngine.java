package org.dromara.northstar.ai.sampling;

import java.io.File;

import org.dromara.northstar.ai.SamplingAware;
import org.dromara.northstar.common.ObjectManager;
import org.dromara.northstar.strategy.IModule;
import org.dromara.northstar.strategy.TradeStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 监控学习数据采样器。采样器会把回调方法注入到策略中，当有K线数据时会触发回调进行采样 
 * 仅用于开发环境
 * @auth KevinHuangwl
 */
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE - 1)
@Component
@Profile("sampling")
public class SamplingEngine implements CommandLineRunner {

	@Autowired
	private ObjectManager<IModule> moduleMgr;
	
	@Override
	public void run(String... args) throws Exception {
		log.info("给模组注入数据采样器");
		moduleMgr.findAll()
			.stream()
			.filter(m -> m.getTradeStrategy() instanceof SamplingAware)
			.forEach(m -> 
				m.getModuleDescription().getModuleAccountSettingsDescription()
					.stream()
					.flatMap(mad -> mad.getBindedContracts().stream())
					.filter(csi -> csi.getName().contains("指数"))
					.findAny()
					.ifPresent(csi -> {
						TradeStrategy ts = m.getTradeStrategy();
						SamplingAware sa = (SamplingAware) ts;
						File csvFile = new File(String.format("data/%s_%dm_%s.csv", ts.name(), m.getModuleContext().numOfMinPerMergedBar(), csi.getName()));
						SampleDataWriter writer = new SampleDataWriter(csvFile);
						sa.setOnBarCallback(writer::append);
						log.info("模组[{}] 注入数据采样器", m.getName());
					})
			);
	}
}


