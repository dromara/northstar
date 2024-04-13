package org.dromara.northstar.strategy;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.dromara.northstar.common.constant.FieldType;
import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.Setting;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.common.utils.FieldUtils;
import org.dromara.northstar.common.utils.TradeHelper;
import org.slf4j.Logger;

import cn.hutool.core.lang.Assert;
import lombok.Getter;
import lombok.Setter;

@StrategicComponent(OpenningHelperStrategy.NAME)
public class OpenningHelperStrategy extends AbstractStrategy implements TradeStrategy{

	public static final String NAME = "开仓助手";
	
	InitParams params;	// 策略的参数配置信息 
	
	Contract tradeContract;
	Contract indexContract;
	
	TradeHelper helper;
	
	Double buyTrigger;
	Double sellTrigger;
	Double buyStop;
	Double sellStop;
	
	Logger logger;
	
	@Override
	protected void initIndicators() {
		logger = ctx.getLogger(getClass());
		List<Contract> contracts = ctx.bindedContracts();
		if(contracts.size() == 1){
			tradeContract = contracts.get(0);
		} else if(contracts.size() == 2){
			contracts.stream().filter(c -> c.name().contains("指数")).findAny().ifPresent(c -> indexContract = c);
			contracts.stream().filter(c -> c.tradable()).findAny().ifPresent(c -> tradeContract = c);
			Assert.notNull(indexContract, "指数合约为空");
			Assert.notNull(tradeContract, "交易合约为空");
		} else {
			throw new IllegalArgumentException("只能绑定不多于两个合约");
		}
		
		helper = TradeHelper.builder().context(ctx).tradeContract(tradeContract).build();
		logger = ctx.getLogger(getClass());
		
		if(params.buyTriggerPrice > 0) 
			buyTrigger = params.buyTriggerPrice;
		if(params.sellTriggerPrice > 0) 
			sellTrigger = params.sellTriggerPrice;
		if(params.buyStopPrice > 0)
			buyStop = params.buyStopPrice;
		if(params.sellStopPrice > 0)
			sellStop = params.sellStopPrice;
	}
	
	@Override
	public void onTick(Tick tick) {
		Contract c = Optional.ofNullable(indexContract).orElse(tradeContract);
		if(!c.equals(tick.contract())) {
			return;
		}
		if(ctx.getState().isEmpty()) {
			if(Objects.nonNull(buyTrigger) && tick.lastPrice() <= buyTrigger && tick.lastPrice() > params.buyStopPrice) {
				helper.doBuyOpen(ctx.getDefaultVolume());
				return;
			}
			if(Objects.nonNull(sellTrigger) && tick.lastPrice() >= sellTrigger && tick.lastPrice() < params.sellStopPrice) {
				helper.doSellOpen(ctx.getDefaultVolume());
				return;
			}
		}
		if(ctx.getState() == ModuleState.HOLDING_SHORT && Objects.nonNull(sellStop) && tick.lastPrice() >= sellStop) {
			helper.doBuyClose(ctx.getDefaultVolume());
		}
		if(ctx.getState() == ModuleState.HOLDING_LONG && Objects.nonNull(buyStop) && tick.lastPrice() <= buyStop) {
			helper.doSellClose(ctx.getDefaultVolume());
		}
	}
	
	@Override
	public void onTrade(Trade trade) {
		if(params.reverse && FieldUtils.isClose(trade.offsetFlag())) {
			switch(trade.direction()) {
			case D_Buy -> helper.doBuyOpen(trade.volume());
			case D_Sell -> helper.doSellOpen(trade.volume());
			default -> throw new IllegalArgumentException("Unexpected value: " + trade.direction());
			}
		}
	}

	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
		this.params = (InitParams) params;
	}
	
	@Setter
	@Getter
	public static class InitParams extends DynamicParams {			
		
		@Setting(label="多头触发价", order=1)
		private double buyTriggerPrice;
		
		@Setting(label="多头止损价", order=2)
		private double buyStopPrice;

		@Setting(label="空头触发价", order=3)
		private double sellTriggerPrice;
		
		@Setting(label="空头止损价", order=4)
		private double sellStopPrice;
		
		@Setting(label="反手", type = FieldType.SELECT, options = {"是", "否"}, optionsVal = {"true", "false"}, order=5)
		private boolean reverse;
	}
	
	@Override
	public String name() {
		return NAME;
	}
}
