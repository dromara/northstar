package tech.xuanwu.northstar.strategy.cta.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tech.xuanwu.northstar.strategy.common.Dealer;
import tech.xuanwu.northstar.strategy.common.RiskControlRule;
import tech.xuanwu.northstar.strategy.common.SignalPolicy;
import tech.xuanwu.northstar.strategy.cta.module.dealer.CtaDealer;
import tech.xuanwu.northstar.strategy.cta.module.risk.DailyDealLimitedRule;
import tech.xuanwu.northstar.strategy.cta.module.risk.PriceExceededRule;
import tech.xuanwu.northstar.strategy.cta.module.risk.TimeExceededRule;
import tech.xuanwu.northstar.strategy.cta.module.risk.UseMarginExceededRule;
import tech.xuanwu.northstar.strategy.cta.module.signal.SampleSignalPolicy;
import tech.xuanwu.northstar.strategy.cta.module.signal.SampleSignalPolicy2;

@Configuration
public class CtaMetaAutoConfiguration {
	
	@Bean
	public Dealer ctaDealer() {
		return new CtaDealer();
	}
	
	@Bean
	public RiskControlRule dailyDealLimitedRule() {
		return new DailyDealLimitedRule();
	}
	
	@Bean
	public RiskControlRule priceExceededRule() {
		return new PriceExceededRule();
	}
	
	@Bean
	public RiskControlRule timeExceededRule() {
		return new TimeExceededRule();
	}
	
	@Bean
	public RiskControlRule useMarginExceededRule() {
		return new UseMarginExceededRule();
	}

	@Bean
	public SignalPolicy demoPolicy() {
		return new SampleSignalPolicy();
	}
	
	@Bean
	public SignalPolicy demoPolicy2() {
		return new SampleSignalPolicy2();
	}
}
