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
	
	private int maxLen;
	
	public RunningMeanAlgo(int maxLen) {
		this.maxLen = maxLen;
		data = new double[maxLen];
	}

	@Override
	public void init(double[] data) {
		Assert.isTrue(maxLen >= data.length, "初始化数据量超过预期");
		System.arraycopy(data, 0, data, 0, data.length);
		this.nextUpdateCursor = data.length;
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
		result += deltaVal / maxLen;
		data[nextUpdateCursor] = val;
		nextUpdateCursor = (nextUpdateCursor + 1) % maxLen;
	}

}
