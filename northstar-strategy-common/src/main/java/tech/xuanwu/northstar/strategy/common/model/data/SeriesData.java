package tech.xuanwu.northstar.strategy.common.model.data;

/**
 * 序列数据
 * 对数组进行封装，方便回溯访问
 * @author KevinHuangwl
 *
 * @param <T>
 */
public class SeriesData<T extends Number> {
	
	private double[] ringBuf;
	private int nextIndex;

	/**
	 * 
	 * @param windowSize	窗口大小
	 */
	public SeriesData(int windowSize) {
		ringBuf = new double[windowSize];
	}
	
	/**
	 * 
	 * @param sourceData	数据源
	 */
	public SeriesData(double[] sourceData) {
		ringBuf = sourceData;
	}
	
	/**
	 * 
	 * @param windowSize	窗口大小
	 * @param sourceData	数据源
	 */
	public SeriesData(int windowSize, double[] sourceData) {
		ringBuf = new double[windowSize];
		int validLength = Math.min(windowSize, sourceData.length);
		System.arraycopy(sourceData, sourceData.length - validLength, ringBuf, 0, validLength);
		nextIndex = (validLength + windowSize) % windowSize;
	}
	
	private int realIndex(int refIndex) {
		return (nextIndex - 1 + ringBuf.length - refIndex) % ringBuf.length;
	}
	
	/**
	 * 回溯N个周期
	 * @param index		0代表最新值，1代表次新值，如此类推
	 * @return
	 */
	public double ref(int index) {
		return ringBuf[realIndex(index)];
	}
	
	/**
	 * 更新 
	 * @param val	
	 */
	public void update(double val) {
		ringBuf[realIndex(0)] = val;
	}
	
	/**
	 * 队头插入，同时会自动丢弃队尾的旧数据
	 * @param val
	 * @return 		返回旧数据
	 */
	public double offer(double val) {
		double oldVal = ringBuf[realIndex(-1)];
		ringBuf[realIndex(-1)] = val;
		return oldVal;
	}
}
