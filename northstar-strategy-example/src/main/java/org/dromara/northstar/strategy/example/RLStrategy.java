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
	
	protected static final String NAME = "示例-RL策略";
	
	private InitParams params;

	private boolean isTrain;

	private String getActionUrl = "http://localhost:5001/get-action";

	private CloseableHttpClient httpClient = HttpClients.createDefault();

	private float lastReward = 0;

	private float beginTotalAsset = 100000;

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
		
		JSONObject jsonData = new JSONObject();
		jsonData.put("open_price", bar.getOpenPrice());
		jsonData.put("high_price", bar.getHighPrice());
		jsonData.put("low_price", bar.getLowPrice());
		jsonData.put("close_price", bar.getClosePrice());
		jsonData.put("last_reward", lastReward);
		String jsonContent = jsonData.toString();
        HttpPost httpPost = new HttpPost(getActionUrl);


		try {
			StringEntity entity = new StringEntity(jsonContent);
			httpPost.setEntity(entity);
			CloseableHttpResponse response = httpClient.execute(httpPost);
			HttpEntity responseEntity = response.getEntity();

			if (responseEntity != null) {
				String jsonResponse = EntityUtils.toString(responseEntity);
				JSONObject jsonObject = JSON.parseObject(jsonResponse);
				Integer actionID = jsonObject.getInteger("action"); // 1: 买；0: 持仓；-1: 卖
				log.info("actionID: {}", actionID);

				// 根据actionID执行交易
				executeTrade(bar, actionID);

				// 记录reward
				lastReward = getReward(bar, actionID);

			}
		} catch (ParseException | IOException e) {
			e.printStackTrace();
		}
	}

	private void executeTrade(BarField bar, int actionID) {
		switch (ctx.getState()) {
			case EMPTY -> {
				if (actionID == 1) {
					ctx.submitOrderReq(TradeIntent.builder()
							.contract(ctx.getContract(bar.getUnifiedSymbol()))
							.operation(SignalOperation.BUY_OPEN)
							.priceType(PriceType.OPP_PRICE)
							.volume(1)
							.timeout(5000)
							.build());
					log.info("多开");
				} else if (actionID == -1) {
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
				if (actionID == -1) {
					ctx.submitOrderReq(TradeIntent.builder()
							.contract(ctx.getContract(bar.getUnifiedSymbol()))
							.operation(SignalOperation.SELL_CLOSE)
							.priceType(PriceType.OPP_PRICE)
							.volume(1)
							.timeout(5000)
							.build());
					log.info("平多");
				}
			}
			case HOLDING_SHORT -> {
				if (actionID == 1) {
					ctx.submitOrderReq(TradeIntent.builder()
							.contract(ctx.getContract(bar.getUnifiedSymbol()))
							.operation(SignalOperation.BUY_CLOSE)
							.priceType(PriceType.OPP_PRICE)
							.volume(1)
							.timeout(5000)
							.build());
					log.info("平空");
				}
			}
			default -> {
				log.info("当前状态：{}，不交易", ctx.getState());
			}
		}
	}

	private int getReward(BarField bar, int actionID){
		return 0;
	}
	
	/***************** 以下如果看不懂，基本可以照搬 *************************/
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
