package tech.xuanwu.northstar.strategy.cta.module.signal;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

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
 * 该消息策略与PA消息策略的区别在于，该策略的消息解析方式不同，匹配的字符串模式不同，相比之下该消息文本更简洁
 * 开仓示例：rb2210:多开3124,止损3100
 * 平仓示例：rb2210:空平4000
 * 反手示例：rb2210:反手开多
 * 
 * ## 风险提示：该策略仅作技术分享，据此交易，风险自担 ##
 * @author KevinHuangwl
 *
 */
@Slf4j
@StrategicComponent("通用消息策略")
public class ExtSignalPolicy extends AbstractSignalPolicy implements ExternalSignalPolicy{
	
	protected Queue<Signal> signalQ = new LinkedList<>();
	
	private Pattern textPtn;
	
	private Pattern oprPtn = Pattern.compile("[:|：](..)([0-9\\.]+)([,|，]止损([0-9\\.]+))?");
	private Pattern revPtn = Pattern.compile("[:|：]反手(开.)([0-9\\\\.]+)");
	
	@Override
	public void onExtMsg(String text) {
		if(textPtn.matcher(text).matches()) {
			String price = "";
			String action = "";
			String stopPrice = "0";
			Matcher revMatcher = revPtn.matcher(text);
			if(revMatcher.find()) {
				action = revMatcher.group(1);
				price = revMatcher.group(2);
				signalQ.offer(makeSignal(action.replace("开", "平"), price, stopPrice));
				signalQ.offer(makeSignal(action, price, stopPrice));
				return;
			}
			Matcher oprMatcher = oprPtn.matcher(text);
			if(oprMatcher.find()) {
				action = oprMatcher.group(1);
				price = oprMatcher.group(2);
				stopPrice = oprMatcher.group(4);
				stopPrice = StringUtils.isEmpty(stopPrice) ? "0" : stopPrice;
				signalQ.offer(makeSignal(action, price, stopPrice));
			}
		}
	}
	
	private Signal makeSignal(String action, String price, String stopPrice) {
		return CtaSignal.builder()
				.id(UUID.randomUUID())
				.sourceUnifiedSymbol(bindedUnifiedSymbol)
				.signalClass(this.getClass())
				.signalPrice(Double.parseDouble(price))
				.stopPrice(Double.parseDouble(stopPrice))
				.state(SignalOperation.parse(action))
				.build();
	}
	
	@Override
	protected Optional<Signal> onTick(int millicSecOfMin, BarData barData) {
		if(signalQ.isEmpty()) {
			return Optional.empty();
		}
		Signal signal = signalQ.poll();
		// 当信号为平仓且当前仓位无持仓时，抛弃该信号
		if(signal != null && !signal.isOpening() && moduleStatus.at(ModuleState.EMPTY)) {
			signal = signalQ.poll();
		}
		return Optional.ofNullable(signal);
	}

	
	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
		InitParams initParams = (InitParams) params;
		Assert.hasText(initParams.bindedUnifiedSymbol, "合约入参为空");
		bindedUnifiedSymbol = initParams.bindedUnifiedSymbol;
		String symbol = bindedUnifiedSymbol.split("@")[0].toUpperCase();
		log.info("绑定合约：{}", symbol);
		textPtn = Pattern.compile("^" + symbol + "[:|：].+$", Pattern.CASE_INSENSITIVE);
	}
	
	public static class InitParams extends DynamicParams{

		@Setting(value="绑定合约", order=10)	
		protected String bindedUnifiedSymbol;	
	}

	

}
