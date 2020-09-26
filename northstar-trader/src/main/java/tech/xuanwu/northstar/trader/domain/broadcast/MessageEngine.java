package tech.xuanwu.northstar.trader.domain.broadcast;

import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.NoticeField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 广播引擎
 * @author kevinhuangwl
 *
 */
public interface MessageEngine {

	void emitTick(TickField tick);
	
	void emitBar(BarField bar);
	
	void emitAccount(AccountField account);
	
	void emitPosition(PositionField position);
	
	void emitTrade(TradeField trade);
	
	void emitOrder(OrderField order);
	
	void emitContract(ContractField contract);
	
	void emitNotice(NoticeField notice);
}
