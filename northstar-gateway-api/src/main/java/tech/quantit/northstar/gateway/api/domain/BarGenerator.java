package tech.quantit.northstar.gateway.api.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.gateway.api.domain.time.OpenningMinuteClock;
import tech.quantit.northstar.gateway.api.domain.time.PeriodHelperFactory;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 1分钟Bar生成器
 * 需要考虑以下几种情况：
 * 情况一：不同的TICK每分钟反馈的TICK数据不同，例如部分合约每秒两个TICK，部分合约每秒多于两个TICK；开始计算的时间也不尽相同，例如部分合约第一个TICK是0分500毫秒，部分合约第一个TICK是0分0毫秒；
 * 情况二：假如行情运行期间断线重连，离线间隔超过原有K线，那么应该先结束原有K线，然后重新生成新K线
 * 情况三：在非行情运行时间，也有可能收到无效的TICK数据，需要做忽略处理
 */
@Slf4j
public class BarGenerator {
	
	private BarField.Builder barBuilder;

	private static final long MAX_TIME_GAP = 90000; //90秒TICK过期判定

	private LocalTime cutoffTime;
	
	private NormalContract contract;
	
	private Consumer<BarField> barCallBack;
	
	private OpenningMinuteClock clock;
	
	private TickField lastTick;
	
	public BarGenerator(NormalContract contract, Consumer<BarField> barCallBack, PeriodHelperFactory phFactory) {
		this.barCallBack = barCallBack;
		this.contract = contract;
		this.clock = new OpenningMinuteClock(contract.contractField(), phFactory);
	}
	
	/**
	 * 更新Tick数据
	 * 
	 * @param tick
	 */
	public synchronized void update(TickField tick) {
		// 如果tick为空或者合约不匹配则返回
		if (tick == null || !contract.unifiedSymbol().equals(tick.getUnifiedSymbol())) {
			log.warn("合约不匹配,当前Bar合约{}", contract.unifiedSymbol());
			return;
		}
		if (System.currentTimeMillis() - tick.getActionTimestamp() > MAX_TIME_GAP) {
			log.debug("忽略过期数据: {} {} {}", tick.getUnifiedSymbol(), tick.getActionDay(), tick.getActionTime());
			return;
		}
		
		// 忽略非行情数据
		if(tick.getStatus() < 1) {
			return;
		}
		
		lastTick = tick;
		
		if(Objects.isNull(cutoffTime)) {
			cutoffTime = clock.barMinute(tick);
		}
		
		if(!clock.isEndOfSection(cutoffTime) && tickTime(tick).isAfter(cutoffTime)) {
			finishOfBar();
			cutoffTime = clock.nextBarMinute();
		}
		if(Objects.isNull(barBuilder)) {
			LocalDateTime barTime = LocalDateTime.of(LocalDate.parse(tick.getActionDay(), DateTimeConstant.D_FORMAT_INT_FORMATTER), cutoffTime);
			barBuilder = BarField.newBuilder()
					.setGatewayId(contract.contractField().getGatewayId())
					.setUnifiedSymbol(contract.unifiedSymbol())
					.setTradingDay(tick.getTradingDay())
					.setOpenPrice(tick.getLastPrice())
					.setHighPrice(tick.getLastPrice())
					.setLowPrice(tick.getLastPrice())
					.setPreClosePrice(tick.getPreClosePrice())
					.setPreOpenInterest(tick.getPreOpenInterest())
					.setPreSettlePrice(tick.getPreSettlePrice())
					.setActionTimestamp(barTime.toInstant(ZoneOffset.ofHours(8)).toEpochMilli())
					.setActionDay(tick.getActionDay())
					.setActionTime(cutoffTime.format(DateTimeConstant.T_FORMAT_FORMATTER));
		}
		
		barBuilder.setHighPrice(Math.max(tick.getLastPrice(), barBuilder.getHighPrice()));
		barBuilder.setLowPrice(Math.min(tick.getLastPrice(), barBuilder.getLowPrice()));
		barBuilder.setClosePrice(tick.getLastPrice());
		barBuilder.setOpenInterest(tick.getOpenInterest());
		barBuilder.setOpenInterestDelta(tick.getOpenInterestDelta() + barBuilder.getOpenInterestDelta());
		barBuilder.setVolume(tick.getVolumeDelta() + barBuilder.getVolume());
		barBuilder.setTurnover(tick.getTurnoverDelta() + barBuilder.getTurnover());
		barBuilder.setNumTrades(tick.getNumTradesDelta() + barBuilder.getNumTrades());
	}
	
	private LocalTime tickTime(TickField tick) {
		return LocalTime.parse(tick.getActionTime(), DateTimeConstant.T_FORMAT_WITH_MS_INT_FORMATTER);
	}
	
	/**
	 * 分钟收盘生成
	 * @return
	 */
	public synchronized BarField finishOfBar() {
		if(Objects.isNull(barBuilder)) {
			return null;
		}
		barBuilder.setVolume(Math.max(0, barBuilder.getVolume()));				// 防止vol为负数
		
		BarField lastBar = barBuilder.build();
		barCallBack.accept(lastBar);
		barBuilder = null;
		cutoffTime = null;
		return lastBar;
	}
	
	/**
	 * 小节收盘检查
	 */
	public synchronized void endOfBar() {
		if(Objects.isNull(lastTick) || System.currentTimeMillis() - lastTick.getActionTimestamp() < MAX_TIME_GAP) {
			return;
		}
		finishOfBar();
	}
	
}
