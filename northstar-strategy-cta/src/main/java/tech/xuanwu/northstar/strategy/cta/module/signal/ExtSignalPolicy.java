package tech.xuanwu.northstar.strategy.cta.module.signal;

import java.time.LocalTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.strategy.common.ExternalSignalPolicy;
import tech.xuanwu.northstar.strategy.common.annotation.Label;
import tech.xuanwu.northstar.strategy.common.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.common.model.CtaSignal;
import tech.xuanwu.northstar.strategy.common.model.meta.DynamicParams;

/**
 * 该策略通过解析外部传入的一段文本来生成一个交易信号
 * @author KevinHuangwl
 *
 */
@Slf4j
@StrategicComponent("PA消息策略")
public class ExtSignalPolicy extends AbstractSignalPolicy implements ExternalSignalPolicy{

	private Pattern textPtn;
	
	private Pattern spOprPtn = Pattern.compile("在([0-9\\.]+)的价格平空单");
	private Pattern bpOprPtn = Pattern.compile("在([0-9\\.]+)的价格平多单");
	private Pattern bkOprPtn = Pattern.compile("在([0-9\\.]+)的价格开多单，止损价：([0-9\\.]+)");
	private Pattern skOprPtn = Pattern.compile("在([0-9\\.]+)的价格开空单，止损价：([0-9\\.]+)");
	
	private String symbol;
	
	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
		InitParams initParams = (InitParams) params;
		symbol = initParams.symbol;
		textPtn = Pattern.compile("[^：]+：" + symbol + "[.]+，仅供参考。");
	}

	@Override
	protected void onTick(int millicSecOfMin) {
		// Do Nothing
	}

	@Override
	protected void onMin(LocalTime time) {
		// Do Nothing		
	}

	@Override
	public void onExtMsg(String text) {
		if(!textPtn.matcher(text).matches()) {
			return;
		}
		
	}
	
//	private CtaSignal resolveSignal(String text, Pattern ptn) {
//		Matcher m = ptn.matcher(text);
//		
//	}
	
	public static class InitParams extends DynamicParams{

		@Label(value="目标合约", order=10)	// Label注解用于定义属性的元信息
		private String symbol;		
	}

}
