package tech.quantit.northstar.gateway.api.domain;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.common.constant.TickType;
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
	
	private long cutoffTime;

	private NormalContract contract;
	
	private Consumer<BarField> barCallBack;
	
	protected int tickCount;

	public BarGenerator(NormalContract contract, Consumer<BarField> barCallBack) {
		this.barCallBack = barCallBack;
		this.contract = contract;
		this.barBuilder = BarField.newBuilder()
				.setGatewayId(contract.contractField().getGatewayId())
				.setUnifiedSymbol(contract.unifiedSymbol());
	}
	
	public BarGenerator(NormalContract contract) {
		this(contract, null);
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
		
		// 忽略非行情数据
		if(tick.getStatus() < 1) {
			return;
		}
		 
		if(tick.getActionTimestamp() > cutoffTime) {
			long offset = 0;	// K线偏移量
			if(tick.getStatus() == TickType.PRE_OPENING_TICK.getCode()) {
				offset = 60000;	// 开盘前一分钟的TICK是盘前数据，要合并到第一个分钟K线
			}
			if(tickCount > 0) {
				barCallBack.accept(barBuilder.build());
				tickCount = 0;
			}
			long barActionTime = tick.getActionTimestamp() - tick.getActionTimestamp() % 60000L + offset;
			cutoffTime = barActionTime + 60000;
			
			barBuilder.setTradingDay(tick.getTradingDay());
			barBuilder.setActionDay(tick.getActionDay());
			barBuilder.setOpenPrice(tick.getLastPrice());
			barBuilder.setHighPrice(tick.getLastPrice());
			barBuilder.setLowPrice(tick.getLastPrice());
			barBuilder.setPreClosePrice(tick.getPreClosePrice());
			barBuilder.setPreOpenInterest(tick.getPreOpenInterest());
			barBuilder.setPreSettlePrice(tick.getPreSettlePrice());
			barBuilder.setNumTradesDelta(0);
			barBuilder.setOpenInterestDelta(0);
			barBuilder.setTurnoverDelta(0);
			barBuilder.setVolumeDelta(0);
			barBuilder.setActionTimestamp(barActionTime);
			barBuilder.setActionTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(barActionTime), ZoneId.systemDefault()).format(DateTimeConstant.T_FORMAT_WITH_MS_INT_FORMATTER));
		}
		
		tickCount++;
		barBuilder.setHighPrice(Math.max(tick.getLastPrice(), barBuilder.getHighPrice()));
		barBuilder.setLowPrice(Math.min(tick.getLastPrice(), barBuilder.getLowPrice()));
		barBuilder.setClosePrice(tick.getLastPrice());
		barBuilder.setOpenInterest(tick.getOpenInterest());
		barBuilder.setVolume(tick.getVolume());
		barBuilder.setTurnover(tick.getTurnover());

		barBuilder.setVolumeDelta(tick.getVolumeDelta() + barBuilder.getVolumeDelta());
		barBuilder.setVolumeDelta(Math.max(0, barBuilder.getVolumeDelta()));	// 防止volDelta为负数
		barBuilder.setTurnoverDelta(tick.getTurnoverDelta() + barBuilder.getTurnoverDelta());
		barBuilder.setOpenInterestDelta(tick.getOpenInterestDelta() + barBuilder.getOpenInterestDelta());

		if(tick.getStatus() == TickType.CLOSING_TICK.getCode()) {
			barCallBack.accept(barBuilder.build());
			tickCount = 0;
		}
	}
	
	public void setOnBarCallback(Consumer<BarField> callback) {
		barCallBack = callback;
	}

}
