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

@Configuration
public class CtaMetaAutoConfiguration {
	
	@Bean
	// @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public Dealer ctaDealer() {
		return new CtaDealer();
	}
	
	@Bean
	// @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public RiskControlRule dailyDealLimitedRule() {
		return new DailyDealLimitedRule();
	}
	
	@Bean
	// @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public RiskControlRule priceExceededRule() {
		return new PriceExceededRule();
	}
	
	@Bean
	// @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public RiskControlRule timeExceededRule() {
		return new TimeExceededRule();
	}
	
	@Bean
	// @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public RiskControlRule useMarginExceededRule() {
		return new UseMarginExceededRule();
	}

	@Bean
	// @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public SignalPolicy demoPolicy() {
		return new SampleSignalPolicy();
	}
	
}
