package tech.xuanwu.northstar.strategy.cta.module.signal;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.strategy.common.ExternalSignalPolicy;
import tech.xuanwu.northstar.strategy.common.Signal;
import tech.xuanwu.northstar.strategy.common.annotation.Setting;
import tech.xuanwu.northstar.strategy.common.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.common.constants.SignalOperation;
import tech.xuanwu.northstar.strategy.common.model.CtaSignal;
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
	
	private Map<Integer, SignalOperation> signalStateMap = new HashMap<>() {
		private static final long serialVersionUID = 1L;

		{
			put(SignalOperation.BuyClose.code(), SignalOperation.BuyClose);
			put(SignalOperation.SellClose.code(), SignalOperation.SellClose);
			put(SignalOperation.BuyOpen.code(), SignalOperation.BuyOpen);
			put(SignalOperation.SellOpen.code(), SignalOperation.SellOpen);
			put(SignalOperation.ReversingBuy.code(), SignalOperation.ReversingBuy);
			put(SignalOperation.ReversingSell.code(), SignalOperation.ReversingSell);
		}
	};
	
	private String symbol;
	
	private volatile String externalText;
	
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
		if(StringUtils.isNotBlank(externalText)) {
			Optional<Signal> signal = Optional.of(resolveSignal(externalText));
			externalText = null;
			return signal;
		}
		return Optional.empty();
	}

	@Override
	public void onExtMsg(String text) {
		if(!textPtn.matcher(text).matches()) {
			return;
		}
		log.info("收到外部指令：{}", text);
		externalText = text;
	}
	
	private CtaSignal resolveSignal(String text) {
		Matcher m1 = spOprPtn.matcher(text);
		Matcher m2 = bpOprPtn.matcher(text);
		String closePrice = "";
		SignalOperation closeState = SignalOperation.None;
		if(m1.find()) {
			closeState = SignalOperation.SellClose;
			closePrice = m1.group(1);
		} else if(m2.find()) {
			closeState = SignalOperation.BuyClose;
			closePrice = m2.group(1);
		}
		
		Matcher m3 = bkOprPtn.matcher(text);
		Matcher m4 = skOprPtn.matcher(text);
		String openPrice = "";
		SignalOperation openState = SignalOperation.None;
		if(m3.find()) {
			openState = SignalOperation.BuyOpen;
			openPrice = m3.group(1);
		}else if(m4.find()) {
			openState = SignalOperation.SellOpen;
			openPrice = m4.group(1);
		}
		
		SignalOperation finalState = signalStateMap.get(openState.code() | closeState.code());
		if(finalState == null) {
			return null;
		}
		
		return CtaSignal.builder()
				.id(UUID.randomUUID())
				.signalClass(this.getClass())
				.signalPrice(choosePrice(finalState.isBuy(), openPrice, closePrice))
				.state(finalState)
				.timestamp(System.currentTimeMillis())
				.build();
	}
	
	public double choosePrice(boolean isBuy, String p1, String p2) {
		if(StringUtils.isAllBlank(p1, p2)) {
			throw new IllegalArgumentException("价格为空");
		}
		if(StringUtils.isEmpty(p1)) {
			return Double.parseDouble(p2);
		}
		if(StringUtils.isEmpty(p2)) {
			return Double.parseDouble(p1);
		}
		return isBuy ? Math.max(Double.parseDouble(p1), Double.parseDouble(p2)) : Math.min(Double.parseDouble(p1), Double.parseDouble(p2));
	}
	
	public static class InitParams extends DynamicParams{

		@Setting(value="绑定合约", order=10)	// Label注解用于定义属性的元信息
		protected String unifiedSymbol;		// 属性可以为任意多个，当元素为多个时order值用于控制前端的显示顺序	
	}

}
