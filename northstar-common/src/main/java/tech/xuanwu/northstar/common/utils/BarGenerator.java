package tech.xuanwu.northstar.common.utils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.constant.DateTimeConstant;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 1分钟Bar生成器
 */
@Slf4j
public class BarGenerator {
	
	private BarField.Builder barBuilder = null;
	private LocalDateTime barLocalDateTime = null;

	private TickField preTick = null;

	private boolean newFlag = false;

	private String barUnifiedSymbol = null;
	
	CommonBarCallBack commonBarCallBack;

	public BarGenerator(String unifiedSymbol, CommonBarCallBack commonBarCallBack) {
		this.commonBarCallBack = commonBarCallBack;
		this.barUnifiedSymbol = unifiedSymbol;
	}
	
	private ConcurrentLinkedQueue<TickField> bufTickList = new ConcurrentLinkedQueue<>();

	/**
	 * 更新Tick数据
	 * 
	 * @param tick
	 */
	public void updateTick(TickField tick) {
		// 如果tick为空或者合约不匹配则返回
		if (tick == null) {
			log.warn("输入的Tick数据为空,当前Bar合约{}",barUnifiedSymbol);
			return;
		}

		if (barUnifiedSymbol == null) {
			barUnifiedSymbol = tick.getUnifiedSymbol();
		} else if (!barUnifiedSymbol.equals(tick.getUnifiedSymbol())) {
			log.warn("合约不匹配,当前Bar合约{}",barUnifiedSymbol);
			return;
		}

		LocalDateTime tickLocalDateTime = CommonUtils.millsToLocalDateTime(tick.getActionTimestamp());
		
		if (preTick != null) {
			// 如果切换交易日
			if (!preTick.getTradingDay().equals(tick.getTradingDay())) {
				preTick = null;
				if(barBuilder!=null){
					finish();
				}
			}else if(!preTick.getActionDay().equals(tick.getActionDay())) {
				if(barBuilder!=null){
					finish();
				}
			}
		}

		if (barBuilder == null) {
			barBuilder = BarField.newBuilder();
			newFlag = true;
		} else if (barLocalDateTime.get(ChronoField.MINUTE_OF_DAY) != tickLocalDateTime.get(ChronoField.MINUTE_OF_DAY) 
				|| (preTick != null && !preTick.getTradingDay().equals(tick.getTradingDay()))) {
			finish();
			barBuilder = BarField.newBuilder();
		} else {
			newFlag = false;
		}

		if (newFlag) {
			barBuilder.setUnifiedSymbol(tick.getUnifiedSymbol());
			barBuilder.setGatewayId(tick.getGatewayId());
			barBuilder.setTradingDay(tick.getTradingDay());
			barBuilder.setActionDay(tick.getActionDay());

			barBuilder.setOpenPrice(tick.getLastPrice());
			barBuilder.setHighPrice(tick.getLastPrice());
			barBuilder.setLowPrice(tick.getLastPrice());

			barLocalDateTime = tickLocalDateTime;
		} else {
			// 当日最高价发生变动
			if (preTick != null && !CommonUtils.isEquals(tick.getHighPrice(), preTick.getHighPrice())) {
				barBuilder.setHighPrice(tick.getHighPrice());
			} else {
				barBuilder.setHighPrice(Math.max(barBuilder.getHighPrice(), tick.getLastPrice()));
			}

			// 当日最低价发生变动
			if (preTick != null && !CommonUtils.isEquals(tick.getLowPrice(), preTick.getLowPrice())) {
				barBuilder.setLowPrice(tick.getLowPrice());
			} else {
				barBuilder.setLowPrice(Math.min(barBuilder.getLowPrice(), tick.getLastPrice()));
			}

		}

		barBuilder.setClosePrice(tick.getLastPrice());
		barBuilder.setOpenInterest(tick.getOpenInterest());
		barBuilder.setVolume(tick.getVolume());
		barBuilder.setTurnover(tick.getTurnover());

		if (preTick != null) {
			barBuilder.setVolumeDelta(tick.getVolume() - preTick.getVolume() + barBuilder.getVolumeDelta());
			barBuilder.setTurnoverDelta(tick.getTurnover() - preTick.getTurnover() + barBuilder.getTurnoverDelta());
			barBuilder.setOpenInterestDelta(tick.getOpenInterest() - preTick.getOpenInterest() + barBuilder.getOpenInterestDelta());
		} else {
			barBuilder.setVolumeDelta(0);
			barBuilder.setTurnoverDelta(0);
			barBuilder.setOpenInterestDelta(tick.getOpenInterest()-tick.getPreOpenInterest());
		}

		preTick = tick;
		bufTickList.add(tick);
	}

	public void finish() {
		if(barBuilder!=null&&barLocalDateTime!=null) {
			barLocalDateTime = barLocalDateTime.withSecond(0).withNano(0);
			barBuilder.setActionTimestamp(CommonUtils.localDateTimeToMills(barLocalDateTime));
			barBuilder.setActionTime(barLocalDateTime.format(DateTimeConstant.T_FORMAT_WITH_MS_INT_FORMATTER));
			
			// 回调OnBar方法
			commonBarCallBack.call(barBuilder.build(), bufTickList);

			// 清空当前Tick缓存
			bufTickList.clear();
			
		}
		
		barLocalDateTime = null;
		barBuilder = null;
		newFlag = true;
	}

	public interface CommonBarCallBack {
		void call(BarField bar, Collection<TickField> minTicks);
	}
}
