package tech.xuanwu.northstar.strategy.trade.strategy;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.alibaba.fastjson.JSON;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.service.ITradeService;
import tech.xuanwu.northstar.strategy.config.common.DefaultSubmitOrderConfig;
import tech.xuanwu.northstar.strategy.config.strategy.BaseStrategyConfig;
import tech.xuanwu.northstar.strategy.constant.TradeState;
import tech.xuanwu.northstar.strategy.msg.MessageClient;
import tech.xuanwu.northstar.strategy.trade.Strategy;
import xyz.redtorch.common.util.bar.BarGenerator;
import xyz.redtorch.common.util.bar.CommonBarCallBack;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

@Slf4j
public abstract class TemplateStrategy implements Strategy, InitializingBean{
	
	@Autowired
	protected BaseStrategyConfig strategyConfig;
	
	@Autowired
	protected DefaultSubmitOrderConfig defaultOrderConfig;

	/*策略运行状态*/
	protected volatile boolean running = false;
	
	/*策略交易状态*/
	protected TradeState tradeState = TradeState.EMPTY_POSITION;
	
	@Value("${northstar.message.endpoint}")
	protected String messageEndpoint;
	
	private ConcurrentHashMap<String, BarGenerator> barGeneratorMap = new ConcurrentHashMap<String, BarGenerator>();
	
	@Autowired
	private ITradeService tradeService;
	
	@Autowired
	private MessageClient msgClient;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		System.out.println("#################################");
		System.out.println(String.format("策略名称：%s", strategyConfig.getStrategyName()));
		System.out.println(String.format("交易账户名称：%s", strategyConfig.getAccountId()));
		System.out.println(String.format("订阅合约名称：%s", JSON.toJSONString(strategyConfig.getMdtdContracts())));
		System.out.println(String.format("交易合约名称：%s", JSON.toJSONString(strategyConfig.getMdtdContracts())));
		System.out.println("#################################");
		
		//注册策略
		msgClient.registerStrategy(this);
	}
	
	@Override
	public void resume() {
		running = true;
	}
	
	@Override
	public void suspend() {
		running = false;
	}
	
	@Override
	public boolean isRunning() {
		return running;
	}
	
	CommonBarCallBack barCallback = (barField)->{
		onBar(barField);
	};
	
	@Override
	public void updateTick(TickField tick) {
		//优先传入onTick计算策略
		onTick(tick);
		
		//再计算Bar
		String contractId = tick.getUnifiedSymbol();
		if(!barGeneratorMap.containsKey(contractId)) {
			barGeneratorMap.put(contractId, new BarGenerator(barCallback)); 
		}
		
		barGeneratorMap.get(contractId).updateTick(tick);
	}
	
	
	protected abstract void onTick(TickField tick);
	
	protected abstract void onBar(BarField bar);
	
	
	private void onOpening(String originOrderId) {
		tradeState = TradeState.OPENNING_POSITION;
	}
	
	private void onClosing(String originOrderId) {
		tradeState = TradeState.CLOSING_POSITION;
	}
	
	
}
