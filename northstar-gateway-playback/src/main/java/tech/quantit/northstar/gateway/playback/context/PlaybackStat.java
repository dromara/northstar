package tech.quantit.northstar.gateway.playback.context;

import lombok.Setter;
import org.apache.commons.math3.stat.StatUtils;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.common.model.PlaybackDescription;
import tech.quantit.northstar.strategy.api.model.ModuleDealRecord;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.ToDoubleFunction;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

/**
 * 回测统计计算
 * @author KevinHuangwl
 *
 */
@Setter
public class PlaybackStat {

	private PlaybackDescription playbackDescription;

	private List<ModuleDealRecord> dealRecords;

	public PlaybackStat(PlaybackDescription playbackDescription, List<ModuleDealRecord> dealRecords) {
		this.playbackDescription = playbackDescription;
		this.dealRecords = dealRecords;
	}

	/**
	 * 计算总盈亏
	 * @return
	 */
	public int sumOfProfit() {
		return dealRecords.stream()
				.mapToInt(ModuleDealRecord::getCloseProfit)
				.sum();
	}

	/**
	 * 计算总交易手续费
	 * @return
	 */
	public int sumOfCommission() {
		return dealRecords.stream()
				.mapToInt(ModuleDealRecord::getVolume)
				.map(v -> v * playbackDescription.getFee())
				.sum();
	}

	/**
	 * 计算交易次数
	 * @return
	 */
	public int timesOfTransaction() {
		return dealRecords.size();
	}

	/**
	 * 计算回测的时间跨度
	 * @return
	 */
	public int duration() {
		LocalDate startDate = LocalDate.parse(playbackDescription.getStartDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER);
		LocalDate endDate = LocalDate.parse(playbackDescription.getEndDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER);
		return (int) startDate.until(endDate.plusDays(1), ChronoUnit.DAYS);
	}

	/**
	 * 计算年化收益率
	 * @return
	 */
	public double yearlyEarningRate() {
		return 365.0 * (sumOfProfit() - sumOfCommission()) / (duration() * meanOfOccupiedMoney());
	}

	/**
	 * 计算平均占用资金
	 * @return
	 */
	public double meanOfOccupiedMoney() {
		return StatUtils.mean(dealRecords.stream().mapToDouble(ModuleDealRecord::getEstimatedOccupiedMoney).toArray());
	}

	/**
	 * 计算总体标准差
	 * @return
	 */
	public double stdOfPlaybackProfits() {
		double[] closeProfits = dealRecords.stream().mapToDouble(ModuleDealRecord::getCloseProfit).toArray();
		return Math.sqrt(StatUtils.variance(closeProfits));
	}

	/**
	 * 计算N个周期样本盈亏均值的均值
	 * @param n
	 * @return
	 */
	public double meanOfNTransactionsAvgProfit(int n) {
		double[] sampleOfAvgProfit = Stream.of(sampling(n, ModuleDealRecord::getCloseProfit)).mapToDouble(StatUtils::mean).toArray();
		return StatUtils.mean(sampleOfAvgProfit);
	}

	/**
	 * 计算N个周期样本内盈亏均值的标准差
	 * @param n
	 * @return
	 */
	public double stdOfNTransactionsAvgProfit(int n) {
		double[] sampleOfAvgProfit = Stream.of(sampling(n, ModuleDealRecord::getCloseProfit)).mapToDouble(StatUtils::mean).toArray();
		return Math.sqrt(StatUtils.variance(sampleOfAvgProfit)) / Math.sqrt(n);
	}

	/**
	 * 计算N个周期样本内胜率均值的均值
	 * @param n
	 * @return
	 */
	public double meanOfNTransactionsAvgWinningRate(int n) {
		double[] sampleOfWinningRate = Stream.of(sampling(n, ModuleDealRecord::getCloseProfit))
				.mapToDouble(sampleProfits -> DoubleStream.of(sampleProfits).filter(profit -> profit > 0).count() * 1.0 / sampleProfits.length)
				.toArray();
		return StatUtils.mean(sampleOfWinningRate);
	}

	/**
	 * 计算N个周期样本内胜率均值的标准差
	 * @param n
	 * @return
	 */
	public double stdOfNTransactionsAvgWinningRate(int n) {
		double[] sampleOfWinningRate = Stream.of(sampling(n, ModuleDealRecord::getCloseProfit))
				.mapToDouble(sampleProfits -> DoubleStream.of(sampleProfits).filter(profit -> profit > 0).count() * 1.0 / sampleProfits.length)
				.toArray();
		return Math.sqrt(StatUtils.variance(sampleOfWinningRate)) / Math.sqrt(n);
	}

	private double[][] sampling(int sampleSize, ToDoubleFunction<ModuleDealRecord> samplingFunc){
		double[][] samples = new double[numberOfSamples(sampleSize)][sampleSize];
		for(int i=0; i<samples.length; i++) {
			double[] sample = samples[i];
			System.arraycopy(dealRecords.subList(i, i + sampleSize).stream().mapToDouble(samplingFunc).toArray(), 0, sample, 0, sampleSize);
		}
		return samples;
	}

	private int numberOfSamples(int n) {
		if(dealRecords.size() <= n) {
			throw new IllegalArgumentException("传入的样本数量过大，不应该大于总体数量 " + dealRecords.size());
		}
		return dealRecords.size() - n + 1;
	}

	/**
	 * 计算最大回撤金额
	 * @return
	 */
	public double maxFallback() {
		double maxProfit = Double.MIN_VALUE;
		double maxFallback = 0;
		double accProfit = 0;
		for(ModuleDealRecord rec : dealRecords) {
			accProfit += rec.getCloseProfit();
			maxProfit = Math.max(maxProfit, accProfit);
			maxFallback = Math.max(maxProfit - accProfit, maxFallback);
		}
		return maxFallback;
	}

}
