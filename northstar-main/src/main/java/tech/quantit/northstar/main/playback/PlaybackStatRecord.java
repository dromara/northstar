package tech.quantit.northstar.main.playback;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Document
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlaybackStatRecord {
	
	@Id
	private String moduleName;
	/**
	 * 总盈亏
	 */
	private int sumOfProfit;
	/**
	 * 总手续费
	 */
	private int sumOfCommission;
	/**
	 * 交易次数
	 */
	private int timesOfTransaction;
	/**
	 * 时间跨度（天）
	 */
	private int duration;
	/**
	 * 年化收益率
	 */
	private double yearlyEarningRate;
	/**
	 * 平仓盈亏标准差
	 */
	private double stdOfProfit;
	/**
	 * 每10笔交易样本的盈亏均值的均值
	 */
	private double meanOf10TransactionsAvgProfit;
	/**
	 * 每10笔交易样本的盈亏均值的标准差
	 */
	private double stdOf10TransactionsAvgProfit;
	/**
	 * 每10笔交易样本的胜率均值
	 */
	private double meanOf10TransactionsAvgWinningRate;
	/**
	 * 每10笔交易样本的胜率标准差
	 */
	private double stdOf10TransactionsAvgWinningRate;
	/**
	 * 每5笔交易样本的盈亏均值的均值
	 */
	private double meanOf5TransactionsAvgProfit;
	/**
	 * 每5笔交易样本的盈亏均值的标准差
	 */
	private double stdOf5TransactionsAvgProfit;
	/**
	 * 每5笔交易样本的胜率均值
	 */
	private double meanOf5TransactionsAvgWinningRate;
	/**
	 * 每5笔交易样本的胜率标准差
	 */
	private double stdOf5TransactionsAvgWinningRate;
	/**
	 * 最大回撤金额
	 */
	private double maxFallback;
	/**
	 * 平均资金占用
	 */
	private double meanOfOccupiedMoney;
	/**
	 * 异常信息
	 */
	private String exceptionMessage;
}
