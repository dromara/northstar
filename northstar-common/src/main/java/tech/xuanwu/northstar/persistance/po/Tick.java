package tech.xuanwu.northstar.persistance.po;

import java.io.Serializable;
import java.util.List;

import org.springframework.beans.BeanUtils;

import lombok.Data;
import xyz.redtorch.pb.CoreField.TickField;

@Data
public class Tick implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -5049417936929560223L;
	private String unifiedSymbol;
	private String gatewayId;
	private String tradingDay;
	private String actionDay;
	private String actionTime;
	private long actionTimestamp;
	private int status;
	private double lastPrice;
	private double avgPrice;
	private long totalBidVol;
	private long totalAskVol;
	private double weightedAvgBidPrice;
	private double weightedAvgAskPrice;
	private double iopv;
	private double yieldToMaturity;
	private long volumeDelta;
	private long volume;
	private double turnover;
	private double turnoverDelta;
	private long numTrades;
	private long numTradesDelta;
	private double openInterest;
	private double openInterestDelta;
	private double preOpenInterest;
	private double preClosePrice;
	private double settlePrice;
	private double preSettlePrice;
	private double openPrice;
	private double highPrice;
	private double lowPrice;
	private double upperLimit;
	private double lowerLimit;
	
	private List<Double> bidPriceList;
	private List<Double> askPriceList;
	private List<Integer> bidVolumeList;
	private List<Integer> askVolumeList;
	
	public static Tick convertFrom(TickField tick) {
		Tick po = new Tick();
		BeanUtils.copyProperties(tick.toBuilder(), po);
		return po;
	}
	
	public TickField convertTo() {
		TickField.Builder tb = TickField.newBuilder();
		BeanUtils.copyProperties(this, tb);
		tb.addAllAskPrice(askPriceList);
		tb.addAllAskVolume(askVolumeList);
		tb.addAllBidPrice(bidPriceList);
		tb.addAllBidVolume(bidVolumeList);
		return tb.build();
	}
}
