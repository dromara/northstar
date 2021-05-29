package tech.xuanwu.northstar.persistence.po;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 1分钟BAR数据
 * @author KevinHuangwl
 *
 */
@Document
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MinBarDataPO {
	private String unifiedSymbol ;  // 统一合约标识
	private String gatewayId ; // 网关ID
	private String tradingDay ;  // 交易日
	private String actionDay ;  // 业务发生日
	private String actionTime ;  // 时间(HHmmssSSS)
	private long actionTimestamp ;  // 时间戳
	private double openPrice ;  // 开
	private double highPrice ;  // 高
	private double lowPrice ;  // 低
	private double closePrice ;  // 收
	private double openInterest ;  // 最后持仓量
	private double openInterestDelta ;  // 持仓量（Bar）
	private long volume ;  // 最后总成交量
	private long volumeDelta ;  // 成交量（Bar）
	private double turnover ;  // 最后成交总额
	private double turnoverDelta ;  // 成交总额（Bar）
	private long numTrades ;  // 最新成交笔数
	private long numTradesDelta ;  // 成交笔数（Bar）
	private double preOpenInterest ;// 昨持仓
	private double preClosePrice ;  // 前收盘价
	private double preSettlePrice ;  // 昨结算价
	private transient int numOfTicks;
}
