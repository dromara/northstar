package tech.xuanwu.northstar.strategy.cta.module.signal;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.strategy.api.AbstractSignalPolicy;
import tech.xuanwu.northstar.strategy.api.ExternalSignalPolicy;
import tech.xuanwu.northstar.strategy.api.annotation.Setting;
import tech.xuanwu.northstar.strategy.api.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.api.constant.Signal;
import tech.xuanwu.northstar.strategy.api.model.DynamicParams;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 该策略通过解析外部传入的一段文本来生成一个交易信号
 * 
 * ## 风险提示：该策略仅作技术分享，据此交易，风险自担 ##
 * @author KevinHuangwl
 *
 */
@Slf4j
@StrategicComponent("PA消息策略")
public class PAExtSignalPolicy extends AbstractSignalPolicy implements ExternalSignalPolicy{

	private Pattern textPtn;
	
	private Pattern bpOprPtn = Pattern.compile("在([0-9\\.]+)的价格平空单");
	private Pattern spOprPtn = Pattern.compile("在([0-9\\.]+)的价格平多单");
	private Pattern bkOprPtn = Pattern.compile("在([0-9\\.]+)的价格开多单，止损价：([0-9\\.]+)");
	private Pattern skOprPtn = Pattern.compile("在([0-9\\.]+)的价格开空单，止损价：([0-9\\.]+)");
	
	private Queue<Signal> signalQ = new LinkedList<>();
	
	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
		InitParams initParams = (InitParams) params;
		if(StringUtils.isEmpty(initParams.bindedUnifiedSymbol)) {
			throw new IllegalArgumentException("合约入参为空");
		}
		bindedUnifiedSymbol = initParams.bindedUnifiedSymbol;
		String symbol = bindedUnifiedSymbol.split("@")[0].toUpperCase();
		log.info("绑定合约：{}", symbol);
		textPtn = Pattern.compile("[^：]+：" + symbol + ".+，仅供参考。");
	}

//	@Override
//	protected Optional<Signal> onTick(int millicSecOfMin, BarData barData) {
//		if(signalQ.isEmpty()) {
//			return Optional.empty();
//		}
//		Signal signal = signalQ.poll();
//		// 当信号为平仓且当前仓位无持仓时，抛弃该信号
//		if(signal != null && !signal.isOpening() && moduleStatus.at(ModuleState.EMPTY)) {
//			signal = signalQ.poll();
//		}
//		return Optional.ofNullable(signal);
//	}

	@Override
	public void onExtMsg(String text) {
		if(!textPtn.matcher(text).matches()) {
			return;
		}
		log.info("收到外部指令：{}", text);
	}
	
//	private void resolveSignal(String text) {
//		Matcher m1 = spOprPtn.matcher(text);
//		Matcher m2 = bpOprPtn.matcher(text);
//		String closePrice = "";
//		if(m1.find()) {
//			closePrice = m1.group(1);
//			signalQ.offer(genSignal(SignalOperation.SellClose, Double.parseDouble(closePrice)));
//		} else if(m2.find()) {
//			closePrice = m2.group(1);
//			signalQ.offer(genSignal(SignalOperation.BuyClose, Double.parseDouble(closePrice)));
//		}
//		
//		
//		Matcher m3 = bkOprPtn.matcher(text);
//		Matcher m4 = skOprPtn.matcher(text);
//		String openPrice = "";
//		String stopPrice;
//		if(m3.find()) {
//			openPrice = m3.group(1);
//			stopPrice = m3.group(2);
//			signalQ.offer(genSignal(SignalOperation.BuyOpen, Double.parseDouble(openPrice), Double.parseDouble(stopPrice)));
//		}else if(m4.find()) {
//			openPrice = m4.group(1);
//			stopPrice = m4.group(2);
//			signalQ.offer(genSignal(SignalOperation.SellOpen, Double.parseDouble(openPrice), Double.parseDouble(stopPrice)));
//		}
//	}
	
	public static class InitParams extends DynamicParams{

		@Setting(value="绑定合约", order=10)	// Label注解用于定义属性的元信息
		protected String bindedUnifiedSymbol;		// 属性可以为任意多个，当元素为多个时order值用于控制前端的显示顺序	
	}

	@Override
	public String name() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onTick(TickField tick) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onBar(BarField bar) {
		// TODO Auto-generated method stub
		
	}

}
