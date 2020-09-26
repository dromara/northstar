package tech.xuanwu.northstar.strategy.trade.algo;

import org.springframework.util.Assert;

import com.google.common.math.Stats;

import tech.xuanwu.northstar.strategy.trade.RunningAlgo;

/**
 * 均值算法
 * @author kevinhuangwl
 *
 */
public class RunningMeanAlgo implements RunningAlgo{
	
	private double[] data;
	
	private int nextUpdateCursor;
	private double result;
	
	private int sampleSize;
	
	public RunningMeanAlgo(int sampleSize) {
		this.sampleSize = sampleSize;
		data = new double[sampleSize];
	}

	@Override
	public void init(double[] data, int nextUpdateCursor) {
		Assert.isTrue(sampleSize == data.length, "数据样本大小不一致");
		this.data = data;
		this.nextUpdateCursor = nextUpdateCursor;
		result = Stats.meanOf(data);
	}
	
	@Override
	public double getResult() {
		return result;
	}

	@Override
	public void update(double val) {
		double oldVal = data[nextUpdateCursor];
		double deltaVal = val - oldVal;
		result += deltaVal / sampleSize;
		data[nextUpdateCursor] = val;
		nextUpdateCursor = (nextUpdateCursor + 1) % sampleSize;
	}

}
