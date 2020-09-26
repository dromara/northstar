package tech.xuanwu.northstar.persistance.po;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.annotation.Id;

import lombok.Data;

/**
 * 一个PO代表一个合约的所有交易记录
 * @author kevinhuangwl
 *
 */
@Data
public class ContractProfit implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1497676650924930766L;

	@Id
	private String id;
	private String unifiedSymbol;
	private String gatewayId;
	private double accumulateCloseProfit;
	private List<TradePair> tradePairRecords = new ArrayList<>();
	
	public ContractProfit() {}
	
	public ContractProfit(String gatewayId, String unifiedSymbol) {
		this.gatewayId = gatewayId;
		this.unifiedSymbol = unifiedSymbol;
	}
	
	public void updateProfit() {
		Optional<Double> result = tradePairRecords.stream()
			.map(tradePair -> tradePair.getClosingProfit())
			.reduce((a, b) -> a + b);
		accumulateCloseProfit = result.orElseGet(() -> 0D);
	}
}
