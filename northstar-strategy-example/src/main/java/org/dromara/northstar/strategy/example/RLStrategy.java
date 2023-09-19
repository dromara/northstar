package org.dromara.northstar.strategy.example;


import org.dromara.northstar.common.constant.FieldType;
import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.constant.SignalOperation;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.Setting;
import org.dromara.northstar.strategy.IModuleContext;
import org.dromara.northstar.strategy.IModuleStrategyContext;
import org.dromara.northstar.strategy.StrategicComponent;
import org.dromara.northstar.strategy.AbstractStrategy;
import org.dromara.northstar.strategy.TradeStrategy;
import org.dromara.northstar.strategy.constant.PriceType;
import org.dromara.northstar.strategy.model.TradeIntent;
import org.dromara.northstar.rl.agent.RLAgent;

import org.slf4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;


@StrategicComponent(RLStrategy.NAME)
public class RLStrategy extends AbstractStrategy{
	
	protected static final String NAME = "RL策略";
	
	private InitParams params;

	private float lastReward = 0;

	private boolean firstRun = true;

	private RLAgent agent = new RLAgent();

	@Override
	public void onTick(TickField tick) {
		log.info("TICK触发: C:{} D:{} T:{} P:{} V:{} OI:{} OID:{}", 
				tick.getUnifiedSymbol(), tick.getActionDay(), tick.getActionTime(), 
				tick.getLastPrice(), tick.getVolume(), tick.getOpenInterest(), tick.getOpenInterestDelta());
    }

	@Override
	public void onMergedBar(BarField bar) {
		log.debug("策略每分钟触发");
		log.debug("{} K线数据： 开 [{}], 高 [{}], 低 [{}], 收 [{}]", 
				 bar.getUnifiedSymbol(), bar.getOpenPrice(), bar.getHighPrice(), bar.getLowPrice(), bar.getClosePrice());

		if (firstRun) { // 第一次运行，初始化信息
			firstRun = !agent.initInfo(params.indicatorSymbol, params.agentName, params.isTrain, params.modelVersion);
			log.info("firstRun: {}", firstRun);
		} else {
			int actionID = agent.getAction(bar, lastReward);
			executeTrade(bar, actionID);
			lastReward = agent.getReward(bar, actionID);
			log.info("actionID: {}, reward: {}", actionID, lastReward);
		}
	}


	private void executeTrade(BarField bar, int actionID) {
		log.info("execute trade...");
		switch (ctx.getState()) {
			case EMPTY -> {
				if (actionID == 0) {
					log.info("actionID=0, EMPTY, 持仓");
				} else if (actionID == 1) {
					ctx.submitOrderReq(TradeIntent.builder()
							.contract(ctx.getContract(bar.getUnifiedSymbol()))
							.operation(SignalOperation.BUY_OPEN)
							.priceType(PriceType.OPP_PRICE)
							.volume(1)
							.timeout(5000)
							.build());
					log.info("actionID=1, EMPTY, 多开");
				} else if (actionID == 2) {
					ctx.submitOrderReq(TradeIntent.builder()
							.contract(ctx.getContract(bar.getUnifiedSymbol()))
							.operation(SignalOperation.SELL_OPEN)
							.priceType(PriceType.OPP_PRICE)
							.volume(1)
							.timeout(5000)
							.build());
					log.info("空开");
				}
			}
			case HOLDING_LONG -> {
				if (actionID == 0) {
					log.info("actionID=0, HOLDING_LONG, 持仓");
				} else if (actionID == 1) {
					log.info("actionID=1, HOLDING_LONG, 持仓");
				} else if (actionID == 2) {
					ctx.submitOrderReq(TradeIntent.builder()
							.contract(ctx.getContract(bar.getUnifiedSymbol()))
							.operation(SignalOperation.SELL_CLOSE)
							.priceType(PriceType.OPP_PRICE)
							.volume(1)
							.timeout(5000)
							.build());
					log.info("actionID=2, HOLDING_LONG, 平多");
				}
			}
			case HOLDING_SHORT -> {
				if (actionID == 0) {
					log.info("actionID=0, HOLDING_SHORT, 持仓");
				} else if (actionID == 1) {
					ctx.submitOrderReq(TradeIntent.builder()
							.contract(ctx.getContract(bar.getUnifiedSymbol()))
							.operation(SignalOperation.BUY_CLOSE)
							.priceType(PriceType.OPP_PRICE)
							.volume(1)
							.timeout(5000)
							.build());
					log.info("actionID=1, HOLDING_SHORT, 平空");
				} else if (actionID == 2) {
					log.info("actionID=2, HOLDING_SHORT, 持仓");
				}
			}
			default -> {
				log.info("当前状态：{}，不交易", ctx.getState());
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

	public static class InitParams extends DynamicParams {
		@Setting(label="指标合约", order=0)
		private String indicatorSymbol;

		@Setting(label="算法名称", order=1)
		private String agentName;

		@Setting(label="是否训练", order=2)
		private boolean isTrain;

		@Setting(label="模型版本", order=3)
		private String modelVersion;
	}
}