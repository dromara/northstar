package tech.quantit.northstar.domain.gateway;

import lombok.Getter;
import lombok.NoArgsConstructor;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 
 * @author KevinHuangwl
 *
 */
@NoArgsConstructor
public class Contract {

	@Getter
	protected ContractField field;

	public Contract(ContractField field) {
		this.field = field;
	}
	
	public boolean hasTickOf(TickField tick) {
		return field.getUnifiedSymbol().equals(tick.getUnifiedSymbol());
	}
	
	public boolean hasBarOf(BarField bar) {
		return field.getUnifiedSymbol().equals(bar.getUnifiedSymbol());
	}
	
	public boolean hasSubmitReqOf(SubmitOrderReqField submitReq) {
		return field.getUnifiedSymbol().equals(submitReq.getContract().getUnifiedSymbol());
	}
	
	public boolean hasOrderOf(OrderField order) {
		return field.getUnifiedSymbol().equals(order.getContract().getUnifiedSymbol());
	}
	
	public boolean hasTradeOf(TradeField trade) {
		return field.getUnifiedSymbol().equals(trade.getContract().getUnifiedSymbol());
	}
	
	public boolean hasPositionOf(PositionField position) {
		return field.getUnifiedSymbol().equals(position.getContract().getUnifiedSymbol());
	}
	
}
