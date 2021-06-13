package tech.xuanwu.northstar.strategy.common.model.data;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 重新封装bar数据，把bar数据的所有关键值以数列形式重新组装
 * @author KevinHuangwl
 *
 */
@Getter
public class BarData {

	private SeriesData<Double> sHigh;
	private SeriesData<Double> sLow;
	private SeriesData<Double> sOpen;
	private SeriesData<Double> sClose;
	private SeriesData<Double> sVol;
	private SeriesData<Double> sOpenIntest;
	
	private TickField lastTick;
	private BarField lastBar;
	
	public BarData(int windowSize, List<BarField> sourceBars) {
		sHigh = new SeriesData<>(windowSize, sourceBars.stream().mapToDouble(bar -> bar.getHighPrice()).toArray());
		sLow = new SeriesData<>(windowSize, sourceBars.stream().mapToDouble(bar -> bar.getLowPrice()).toArray());
		sOpen = new SeriesData<>(windowSize, sourceBars.stream().mapToDouble(bar -> bar.getOpenPrice()).toArray());
		sClose = new SeriesData<>(windowSize, sourceBars.stream().mapToDouble(bar -> bar.getClosePrice()).toArray());
		sVol = new SeriesData<>(windowSize, sourceBars.stream().mapToDouble(bar -> bar.getVolumeDelta()).toArray());
		sOpenIntest = new SeriesData<>(windowSize, sourceBars.stream().mapToDouble(bar -> bar.getOpenInterestDelta()).toArray());
		lastBar = sourceBars.get(sourceBars.size() - 1);
	}
	
	public BarData(List<BarField> sourceBars) {
		this(sourceBars.size(), sourceBars);
	}
	
	/**
	 * Bar更新
	 * 为了防止数据计算不一致，Bar更新才会对数列进行插入
	 * @param bar
	 */
	public synchronized void update(BarField bar) {
		if(StringUtils.equals(lastBar.getUnifiedSymbol(), bar.getUnifiedSymbol())) {
			return;
		}
		sHigh.update(bar.getHighPrice());
		sLow.update(bar.getLowPrice());
		sOpen.update(bar.getOpenPrice());
		sClose.update(bar.getClosePrice());
		sVol.update(bar.getVolumeDelta());
		sOpenIntest.update(bar.getOpenInterestDelta());

		lastBar = bar;
		
		sHigh.offer(bar.getClosePrice());
		sLow.offer(bar.getClosePrice());
		sOpen.offer(bar.getClosePrice());
		sClose.offer(bar.getClosePrice());
		sVol.offer(0D);
		sOpenIntest.offer(0D);
		
	}
	
	/**
	 * Tick更新
	 * @param tick
	 */
	public synchronized void update(TickField tick) {
		if(StringUtils.equals(lastBar.getUnifiedSymbol(), tick.getUnifiedSymbol())) {
			return;
		}
		if(lastTick != null && tick.getActionTimestamp() == lastTick.getActionTimestamp()) {
			return;
		}
		
		sHigh.update(Math.max(sHigh.ref(0), tick.getLastPrice()));
		sLow.update(Math.min(sLow.ref(0), tick.getLastPrice()));
		sClose.update(tick.getLastPrice());
		sVol.update(tick.getVolume() - lastBar.getVolume());
		sOpenIntest.update(tick.getOpenInterest() - lastBar.getOpenInterest());
		
	}
	
}
