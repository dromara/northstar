package org.dromara.northstar.strategy.example;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.Setting;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.indicator.trend.EMAIndicator;
import org.dromara.northstar.indicator.trend.MAIndicator;
import org.dromara.northstar.strategy.AbstractStrategy;
import org.dromara.northstar.strategy.StrategicComponent;
import org.dromara.northstar.strategy.TradeStrategy;
import org.dromara.northstar.strategy.ai.Action;
import org.dromara.northstar.strategy.ai.AiEnvironment;
import org.dromara.northstar.strategy.ai.RLAware;
import org.dromara.northstar.strategy.ai.Reward;
import org.dromara.northstar.strategy.ai.State;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;

@StrategicComponent(ReinformanceLearningDemoStrategy.NAME)
public class ReinformanceLearningDemoStrategy extends AbstractStrategy implements RLAware, // RLAware定义了强化学习的计算函数
	TradeStrategy {
	
	public static final String NAME = "强化学习示例策略";
	
	InitParams params;	// 策略的参数配置信息
	
	Indicator fastMA;
	Indicator slowMA;
	
	AiEnvironment env;
	
	Table<ModuleState, Action, Runnable> actionTbl = HashBasedTable.create();

	@Override
	protected void initIndicators() {
		ContractField c = ctx.getContract(params.indicatorSymbol);
		fastMA = new MAIndicator(Configuration.builder()
				.contract(c)
				.indicatorName("MA10")
				.numOfUnits(ctx.numOfMinPerMergedBar()).build(), 10);
		slowMA = new EMAIndicator(Configuration.builder()
				.contract(c)
				.indicatorName("MA20")
				.numOfUnits(ctx.numOfMinPerMergedBar()).build(), 20);
		//FIXME 区分新建还是加载
		env = new AiEnvironment(this);
		
		actionTbl.put(ModuleState.EMPTY, Action.BUY, () -> {
			
		});
		actionTbl.put(ModuleState.EMPTY, Action.SELL, () -> {});
		actionTbl.put(ModuleState.HOLDING_LONG, Action.NONE, () -> {});
		actionTbl.put(ModuleState.HOLDING_LONG, Action.SELL, () -> {});
		actionTbl.put(ModuleState.HOLDING_SHORT, Action.NONE, () -> {});
		actionTbl.put(ModuleState.HOLDING_SHORT, Action.BUY, () -> {});
	}
	
	@Override
	public Reward reward() {
		return new Reward() {
			
			double lastProfit;
			
			@Override
			public double evaluate() {
				double freshProfit = ctx.getModuleAccount().totalHoldingProfit();
				double deltaProfit = freshProfit - lastProfit;
				lastProfit = freshProfit;
				return deltaProfit;
			}
		};
	}
	
	@Override
	public State state() {
		return new State() {
			
			@Override
			public JSONObject evaluate() {
				return null;	//FIXME 如何定义数据结构
			}
		};
	}
	
	@Override
	public void onAction(Action action) {
		Optional.ofNullable(actionTbl.get(ctx.getState(), action)).ifPresent(Runnable::run);
	}
	
	@Override
	public AiEnvironment getAiEnvironment() {
		return env;
	}
	
	@Override
	public void onMergedBar(BarField bar) {
		if(!StringUtils.equals(bar.getUnifiedSymbol(), params.indicatorSymbol)) {
			return;
		}
		env.exchange();	// FIXME 区分训练还是应用
	}

	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
		this.params = (InitParams) params;
	}

	public static class InitParams extends DynamicParams {			
		
		@Setting(label="指标合约", order=0)
		private String indicatorSymbol;
		
	}

}
