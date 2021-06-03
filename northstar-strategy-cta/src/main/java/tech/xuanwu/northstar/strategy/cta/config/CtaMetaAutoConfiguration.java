package tech.xuanwu.northstar.strategy.cta.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tech.xuanwu.northstar.strategy.common.SignalPolicy;
import tech.xuanwu.northstar.strategy.cta.signal.SampleSignalPolicy;
import tech.xuanwu.northstar.strategy.cta.signal.SampleSignalPolicy2;

@Configuration
public class CtaMetaAutoConfiguration {

	@Bean(name="示例策略")
	public SignalPolicy demoPolicy() {
		return new SampleSignalPolicy(null, null);
	}
	
	
	@Bean(name="示例策略2")
	public SignalPolicy demoPolicy2() {
		return new SampleSignalPolicy2(null);
	}
}
