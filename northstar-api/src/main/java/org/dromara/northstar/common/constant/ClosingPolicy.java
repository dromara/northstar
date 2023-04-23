package org.dromara.northstar.common.constant;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 平仓策略
 * @author KevinHuangwl
 *
 */
public enum ClosingPolicy {

	/**
	 * 按开仓的时间顺序平仓
	 */
	FIRST_IN_FIRST_OUT("先开先平") {
		
		@Override
		public OffsetFlagEnum resolveOffsetFlag(SignalOperation operation, ContractField contract, List<TradeField> nonclosedTrades,
				String tradingDay) {
			if(operation.isOpen()) {
				return OffsetFlagEnum.OF_Open;
			}
			checkNonclosedTrades(nonclosedTrades, contract);
			nonclosedTrades.sort((a, b) -> a.getTradeTimestamp() < b.getTradeTimestamp() ? -1 : 1);
			if(StringUtils.equals(tradingDay, nonclosedTrades.get(0).getTradingDay())) {
				return OffsetFlagEnum.OF_CloseToday;
			}
			return OffsetFlagEnum.OF_Close;
		}
		
	},
	/**
	 * 按开仓的时间倒序平仓
	 */
	FIRST_IN_LAST_OUT("平今优先") {
		
		@Override
		public OffsetFlagEnum resolveOffsetFlag(SignalOperation operation, ContractField contract, List<TradeField> nonclosedTrades,
				String tradingDay) {
			if(operation.isOpen()) {
				return OffsetFlagEnum.OF_Open;
			}
			checkNonclosedTrades(nonclosedTrades, contract);
			nonclosedTrades.sort((a, b) -> a.getTradeTimestamp() > b.getTradeTimestamp() ? -1 : 1);
			if(StringUtils.equals(tradingDay, nonclosedTrades.get(0).getTradingDay())) {
				return OffsetFlagEnum.OF_CloseToday;
			}
			return OffsetFlagEnum.OF_Close;
		}
		
	},
	/**
	 * 优先平掉历史持仓，对冲锁仓今天的持仓
	 * 注意：这里只有锁仓逻辑，没有解锁逻辑
	 */
	CLOSE_NONTODAY_HEGDE_TODAY("平昨锁今") {
		
		@Override
		public OffsetFlagEnum resolveOffsetFlag(SignalOperation operation, ContractField contract, List<TradeField> nonclosedTrades,
				String tradingDay) {
			if(operation.isOpen()) {
				return OffsetFlagEnum.OF_Open;
			}
			checkNonclosedTrades(nonclosedTrades, contract);
			nonclosedTrades.sort((a, b) -> a.getTradeTimestamp() < b.getTradeTimestamp() ? -1 : 1);
			if(StringUtils.equals(tradingDay, nonclosedTrades.get(0).getTradingDay())) {
				return OffsetFlagEnum.OF_Open;
			}
			return OffsetFlagEnum.OF_Close;
		}
		
	};

	@Getter
	private String name;
	private ClosingPolicy(String name) {
		this.name = name;
	}
	
	protected void checkNonclosedTrades(List<TradeField> nonclosedTrades, ContractField contract) {
		if(Objects.isNull(nonclosedTrades) || nonclosedTrades.isEmpty()) {
			throw new IllegalArgumentException("平仓时，持仓列表不能为空");
		}
		for(TradeField trade : nonclosedTrades) {
			if(!StringUtils.equals(trade.getContract().getContractId(), contract.getContractId())) {
				throw new IllegalArgumentException(String.format("持仓列表中包含其他合约。期望：%s, 实际：%s", contract.getContractId(), trade.getContract().getContractId()) );
			}
		}
	}
	
	public abstract OffsetFlagEnum resolveOffsetFlag(SignalOperation operation, ContractField contract, List<TradeField> nonclosedTrades, String tradingDay);
}
