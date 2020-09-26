package tech.xuanwu.northstar.strategy.constant;

/**
 * 交易状态
 * @author kevinhuangwl
 *
 */
public enum TradeState {

	//空仓
	EMPTY_POSITION,
	//开仓中
	OPENNING_POSITION,
	//持多仓
	LONG_POSITION,
	//持空仓
	SHORT_POSITION,
	//平仓中
	CLOSING_POSITION
}
