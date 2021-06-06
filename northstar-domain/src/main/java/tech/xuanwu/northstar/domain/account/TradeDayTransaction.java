package tech.xuanwu.northstar.domain.account;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 交易日成交
 * @author KevinHuangwl
 *
 */
public class TradeDayTransaction {

	private ConcurrentLinkedQueue<TradeField> tradeQ = new ConcurrentLinkedQueue<>();
	
	/**
	 * 更新成交信息
	 * @param trade
	 */
	public void update(TradeField trade) {
		tradeQ.offer(trade);
	}
	
	/**
	 * 获取当时成交列表
	 * @return
	 */
	public List<TradeField> getTransactions(){
		List<TradeField> result = new ArrayList<>(tradeQ.size());
		tradeQ.stream().forEach(result::add);
		return Collections.unmodifiableList(result);
	}
	
}
