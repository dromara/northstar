package tech.xuanwu.northstar.strategy.trade.strategy;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.strategy.config.strategy.BaseStrategyConfig;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

@Slf4j
@Component
@EnableConfigurationProperties(BaseStrategyConfig.class)
public class DemoStrategy extends TemplateStrategy {
	
	public DemoStrategy(BaseStrategyConfig config) {
		strategyConfig = config;
	}

	@Override
	protected void onTick(TickField tick) {
		log.info("合约-[{}], 价：{}，仓：{}，量：{}", 
				tick.getUnifiedSymbol(),
				tick.getLastPrice(), 
				tick.getOpenInterestDelta(),
				tick.getVolumeDelta());
	}

	@Override
	protected void onBar(BarField bar) {
		log.info("合约-[{}]，开：{}，高：{}，低：{}，收：{}，仓：{}，量：{}",
				bar.getUnifiedSymbol(),
				bar.getOpenPrice(),
				bar.getHighPrice(),
				bar.getLowPrice(),
				bar.getClosePrice(),
				bar.getOpenInterest(),
				bar.getVolume());
	}

}
