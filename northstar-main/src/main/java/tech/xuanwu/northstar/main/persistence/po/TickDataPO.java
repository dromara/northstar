package tech.xuanwu.northstar.main.persistence.po;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.redtorch.pb.CoreField.TickField;

@Document
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TickDataPO {

	private String actionTime ;  // 时间(HHmmssSSS)
	private long actionTimestamp; // 时间戳
	private double askPrice1;	// 卖一价
	private double lastPrice ;  // 最新成交价
	private double avgPrice;
	private double bidPrice1;	// 买一价
	private int askVol1;
	private int bidVol1;
	private long volumeDelta ;  // 成交量变化
	private long volume;
	private double turnoverDelta ;  // 成交总额变化
	private double turnover;
	private long numTradesDelta ;  // 成交笔数
	private long numTrades;
	private double openInterestDelta ;  // 持仓量变化
	private double openInterest;
	
	public static TickDataPO convertFrom(TickField tick) {
		return TickDataPO.builder()
				.actionTime(tick.getActionTime())
				.actionTimestamp(tick.getActionTimestamp())
				.askPrice1(tick.getAskPrice(0))
				.lastPrice(tick.getLastPrice())
				.bidPrice1(tick.getBidPrice(0))
				.avgPrice(tick.getAvgPrice())
				.askVol1(tick.getAskVolume(0))
				.bidVol1(tick.getBidVolume(0))
				.volume(tick.getVolume())
				.volumeDelta(tick.getVolumeDelta())
				.turnover(tick.getTurnover())
				.turnoverDelta(tick.getTurnoverDelta())
				.numTrades(tick.getNumTrades())
				.numTradesDelta(tick.getNumTradesDelta())
				.openInterest(tick.getOpenInterest())
				.openInterestDelta(tick.getOpenInterestDelta())
				.build();
	}
	
}
