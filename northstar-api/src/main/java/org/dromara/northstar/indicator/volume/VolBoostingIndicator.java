package org.dromara.northstar.indicator.volume;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.dromara.northstar.indicator.AbstractIndicator;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.constant.ValueType;
import org.dromara.northstar.indicator.helper.SimpleValueIndicator;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.indicator.model.Num;

/**
 * 成交放量阈值指标
 * @author KevinHuangwl
 *
 */
public class VolBoostingIndicator extends AbstractIndicator implements Indicator{
	
	private int countOfDays;
	
	private int stepCount;
	
	private long lastTradeDateInt;
	
	private Indicator volume;
	
	private LinkedList<List<VolumePoint>> data = new LinkedList<>();
	
	private KMeansPlusPlusClusterer<VolumePoint> kmeans = new KMeansPlusPlusClusterer<>(2, 100);
	
	private double threshold;
	
	public VolBoostingIndicator(Configuration cfg, int countOfDays) {
		super(cfg.toBuilder().valueType(ValueType.TRADE_DATE).build());
		this.countOfDays = countOfDays;
		this.volume = new SimpleValueIndicator(cfg.toBuilder().indicatorName("VOL_origin").visible(false).valueType(ValueType.VOL_DELTA).build());
	}
	
	@Override
	public List<Indicator> dependencies() {
		return List.of(volume);
	}

	@Override
	protected Num evaluate(Num num) {
		long tradeDateInt = Math.round(num.value());
		if(lastTradeDateInt != tradeDateInt) {
			lastTradeDateInt = tradeDateInt;
			stepCount = 0;
			if(data.size() == countOfDays) {
				List<CentroidCluster<VolumePoint>> result = kmeans.cluster(data.stream().flatMap(List::stream).toList());
				double center1 = result.get(0).getCenter().getPoint()[0];
				double center2 = result.get(1).getCenter().getPoint()[0];
				if(center1 > center2) {
					double[] volData = result.get(1).getPoints().stream().mapToDouble(p -> p.getPoint()[0]).toArray();
					double std = new StandardDeviation().evaluate(volData, center2);
					threshold = center2 + 2 * std;
				} else {
					double[] volData = result.get(0).getPoints().stream().mapToDouble(p -> p.getPoint()[0]).toArray();
					double std = new StandardDeviation().evaluate(volData, center1);
					threshold = center1 + 2 * std;
				}
				data.poll();
			}
			data.offer(new ArrayList<>());
		}
		if(stepCount++ > 0) {			
			data.peekLast().add(new VolumePoint(volume.value(0)));
		}
		return Num.of(threshold, num.timestamp(), num.unstable());
	}

	private static class VolumePoint implements Clusterable{
		
		private double[] point;
		
		public VolumePoint(double volume) {
			this.point = new double[] {volume};
		}
		
		@Override
		public double[] getPoint() {
			return point;
		}
		
	}
	
}
