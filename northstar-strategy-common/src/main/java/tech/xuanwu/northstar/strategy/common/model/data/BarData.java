package tech.xuanwu.northstar.strategy.common.model.data;

import java.util.ArrayList;
import java.util.LinkedList;
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
	private final String unifiedSymbol;
	
	private final int windowSize;
	
	private LinkedList<BarField> barFieldList = new LinkedList<>();
	
	public BarData(String unifiedSymbol, int windowSize, List<BarField> sourceBars) {
		if(sourceBars.size() == 0) {
			throw new IllegalArgumentException("历史数据不能为空");
		}
		this.unifiedSymbol = unifiedSymbol;
		sHigh = new SeriesData<>(windowSize, sourceBars.stream().mapToDouble(bar -> bar.getHighPrice()).toArray());
		sLow = new SeriesData<>(windowSize, sourceBars.stream().mapToDouble(bar -> bar.getLowPrice()).toArray());
		sOpen = new SeriesData<>(windowSize, sourceBars.stream().mapToDouble(bar -> bar.getOpenPrice()).toArray());
		sClose = new SeriesData<>(windowSize, sourceBars.stream().mapToDouble(bar -> bar.getClosePrice()).toArray());
		sVol = new SeriesData<>(windowSize, sourceBars.stream().mapToDouble(bar -> bar.getVolumeDelta()).toArray());
		sOpenIntest = new SeriesData<>(windowSize, sourceBars.stream().mapToDouble(bar -> bar.getOpenInterestDelta()).toArray());
		lastBar = sourceBars.get(sourceBars.size() - 1);
		
		this.windowSize = windowSize;
		if(sourceBars.size() <= windowSize) {
			barFieldList.addAll(sourceBars);
		} else {
			barFieldList.addAll(sourceBars.subList(sourceBars.size() - windowSize, sourceBars.size()));
		}
	}
	
	public BarData(String unifiedSymbol, List<BarField> sourceBars) {
		this(unifiedSymbol, sourceBars.size(), sourceBars);
	}
	
	/**
	 * Bar更新
	 * 为了防止数据计算不一致，Bar更新才会对数列进行插入
	 * @param bar
	 */
	public synchronized void update(BarField bar) {
		if(StringUtils.equals(unifiedSymbol, bar.getUnifiedSymbol())) {
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
		
		barFieldList.offerLast(bar);
		if(barFieldList.size() > windowSize) {
			barFieldList.peekFirst();
		}
	}
	
	/**
	 * Tick更新
	 * @param tick
	 */
	public synchronized void update(TickField tick) {
		if(StringUtils.equals(unifiedSymbol, tick.getUnifiedSymbol())) {
			return;
		}
		if(lastTick != null && tick.getActionTimestamp() == lastTick.getActionTimestamp()) {
			return;
		}
		
		sHigh.update(Math.max(sHigh.ref(0), tick.getLastPrice()));
		sLow.update(Math.min(sLow.ref(0), tick.getLastPrice()));
		sClose.update(tick.getLastPrice());
		sVol.update(lastBar != null ? tick.getVolume() - lastBar.getVolume() : 0);
		sOpenIntest.update(lastBar != null ? tick.getOpenInterest() - lastBar.getOpenInterest() : 0);
		
	}
	
	/**
	 * 获取引用数据
	 * @return
	 */
	public List<BarField> getRefBarList(){
		List<BarField> result = new ArrayList<>(barFieldList.size());
		result.addAll(barFieldList);
		return result;
	}
	
}
