package tech.quantit.northstar.gateway.api.domain;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.Constants;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.common.constant.TickType;
import tech.quantit.northstar.common.utils.MessagePrinter;
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
	
	private static final GlobalCutOffTimeHelper helper = new GlobalCutOffTimeHelper();

	private NormalContract contract;
	
	private BiConsumer<BarField, List<TickField>> barCallBack;
	
	private ConcurrentLinkedQueue<TickField> barTicks = new ConcurrentLinkedQueue<>();
	
	public BarGenerator(NormalContract contract, BiConsumer<BarField, List<TickField>> barCallBack) {
		this.barCallBack = barCallBack;
		this.contract = contract;
		this.barBuilder = BarField.newBuilder()
				.setGatewayId(contract.contractField().getGatewayId())
				.setUnifiedSymbol(contract.unifiedSymbol());
		helper.register(this);
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
		 
		if(tick.getActionTimestamp() >= cutoffTime) {
			long offset = 0;	// K线偏移量
			if(tick.getStatus() == TickType.PRE_OPENING_TICK.getCode()) {
				offset = 60000;	// 开盘前一分钟的TICK是盘前数据，要合并到第一个分钟K线
			}
			long barActionTime = tick.getActionTimestamp() - tick.getActionTimestamp() % 60000L + 60000 + offset; // 采用K线的收盘时间作为K线时间
			long newCutoffTime = barActionTime;
			
			if(newCutoffTime != helper.cutoffTime) {
				if(tick.getUnifiedSymbol().contains(Constants.INDEX_SUFFIX)) {
					log.info("new cutoff: {}, helper cutoff: {}", newCutoffTime, helper.cutoffTime);
					log.info("{}", MessagePrinter.print(tick));
				}
				helper.updateCutoffTime(newCutoffTime);
			}
			
			barBuilder.setOpenPrice(tick.getLastPrice());
			barBuilder.setHighPrice(tick.getLastPrice());
			barBuilder.setLowPrice(tick.getLastPrice());
			
			barBuilder.setNumTradesDelta(0);
			barBuilder.setOpenInterestDelta(0);
			barBuilder.setTurnoverDelta(0);
			barBuilder.setVolumeDelta(0);
			barBuilder.setActionTimestamp(barActionTime);
			barBuilder.setActionTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(barActionTime), ZoneId.systemDefault()).format(DateTimeConstant.T_FORMAT_WITH_MS_INT_FORMATTER));
		}
		
		barTicks.offer(tick);
		barBuilder.setPreClosePrice(tick.getPreClosePrice());
		barBuilder.setPreOpenInterest(tick.getPreOpenInterest());
		barBuilder.setPreSettlePrice(tick.getPreSettlePrice());
		barBuilder.setTradingDay(tick.getTradingDay());
		barBuilder.setActionDay(tick.getActionDay());
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
	}
	
	public BarField finishOfBar() {
		if(barTicks.size() < 3) {
			// 若TICK数据少于三个TICK，则不触发回调，因为这不是一个正常的数据集
			return barBuilder.build();
		}
		List<TickField> ticksList = barTicks.stream().toList();
		barTicks.clear();
		BarField bar = barBuilder.build();
		barCallBack.accept(bar, ticksList);
		return bar;
	}
	
	public void setOnBarCallback(BiConsumer<BarField, List<TickField>> callback) {
		barCallBack = callback;
	}

	private static class GlobalCutOffTimeHelper{
		
		private volatile long cutoffTime;
		
		private Set<BarGenerator> registeredSet = new HashSet<>();
		
		synchronized void register(BarGenerator barGen) {
			registeredSet.add(barGen);
		}
		
		synchronized void updateCutoffTime(long newCutoffTime) {
			if(newCutoffTime > cutoffTime) {
				registeredSet.stream().forEach(BarGenerator::finishOfBar);
				LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(newCutoffTime), ZoneId.systemDefault());
				log.trace("下次K线生成时间：{}", ldt.toLocalTime());
			}
			cutoffTime = newCutoffTime;
		}
	}
}
