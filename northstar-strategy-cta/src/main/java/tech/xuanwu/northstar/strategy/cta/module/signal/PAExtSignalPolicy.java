package tech.xuanwu.northstar.strategy.cta.module.signal;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.strategy.common.ExternalSignalPolicy;
import tech.xuanwu.northstar.strategy.common.Signal;
import tech.xuanwu.northstar.strategy.common.annotation.Setting;
import tech.xuanwu.northstar.strategy.common.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.common.constants.ModuleState;
import tech.xuanwu.northstar.strategy.common.constants.SignalOperation;
import tech.xuanwu.northstar.strategy.common.model.data.BarData;
import tech.xuanwu.northstar.strategy.common.model.meta.DynamicParams;

/**
 * 该策略通过解析外部传入的一段文本来生成一个交易信号
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
	
	private String symbol;
	
	private Queue<Signal> signalQ = new LinkedList<>();
	
	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
		InitParams initParams = (InitParams) params;
		if(StringUtils.isEmpty(initParams.unifiedSymbol)) {
			throw new IllegalArgumentException("合约入参为空");
		}
		bindedUnifiedSymbol = initParams.unifiedSymbol;
		symbol = bindedUnifiedSymbol.split("@")[0].toUpperCase();
		log.info("绑定合约：{}", symbol);
		textPtn = Pattern.compile("[^：]+：" + symbol + ".+，仅供参考。");
	}

	@Override
	protected Optional<Signal> onTick(int millicSecOfMin, BarData barData) {
		if(signalQ.size() > 0) {
			Signal signal = signalQ.poll();
			// 当信号为平仓且当前仓位无持仓时，抛弃该信号
			if(signal != null && !signal.isOpening() && moduleStatus.at(ModuleState.EMPTY)) {
				signal = signalQ.poll();
			}
			return Optional.ofNullable(signal);
		}
		return Optional.empty();
	}

	@Override
	public void onExtMsg(String text) {
		if(!textPtn.matcher(text).matches()) {
			return;
		}
		log.info("收到外部指令：{}", text);
		resolveSignal(text);
	}
	
	private void resolveSignal(String text) {
		Matcher m1 = spOprPtn.matcher(text);
		Matcher m2 = bpOprPtn.matcher(text);
		String closePrice = "";
		if(m1.find()) {
			closePrice = m1.group(1);
			signalQ.offer(genSignal(SignalOperation.SellClose, Double.parseDouble(closePrice)));
		} else if(m2.find()) {
			closePrice = m2.group(1);
			signalQ.offer(genSignal(SignalOperation.BuyClose, Double.parseDouble(closePrice)));
		}
		
		
		Matcher m3 = bkOprPtn.matcher(text);
		Matcher m4 = skOprPtn.matcher(text);
		String openPrice = "";
		String stopPrice = "0";
		if(m3.find()) {
			openPrice = m3.group(1);
			stopPrice = m3.group(2);
			signalQ.offer(genSignal(SignalOperation.BuyOpen, Double.parseDouble(openPrice), Double.parseDouble(stopPrice)));
		}else if(m4.find()) {
			openPrice = m4.group(1);
			stopPrice = m4.group(2);
			signalQ.offer(genSignal(SignalOperation.SellOpen, Double.parseDouble(openPrice), Double.parseDouble(stopPrice)));
		}
	}
	
	public static class InitParams extends DynamicParams{

		@Setting(value="绑定合约", order=10)	// Label注解用于定义属性的元信息
		protected String unifiedSymbol;		// 属性可以为任意多个，当元素为多个时order值用于控制前端的显示顺序	
	}

	

}
