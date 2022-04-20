package tech.quantit.northstar.common.utils;

import tech.quantit.northstar.common.constant.SignalOperation;
import tech.quantit.northstar.common.model.OrderRequest.TradeOperation;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

public class OrderUtils {
	
	private OrderUtils() {}
	
	public static DirectionEnum resolveDirection(TradeOperation opr) {
		return opr.toString().charAt(0) == 'B' ? DirectionEnum.D_Buy : DirectionEnum.D_Sell;
	}
	
	public static DirectionEnum resolveDirection(SignalOperation opr) {
		return switch(opr) {
		case BUY_CLOSE, BUY_OPEN -> DirectionEnum.D_Buy;
		case SELL_CLOSE, SELL_OPEN -> DirectionEnum.D_Sell;
		default -> DirectionEnum.D_Unknown;
		};
	}
	
	public static boolean isOpenningOrder(TradeOperation opr) {
		return opr.toString().charAt(1) == 'K';
	}
	
	public static boolean isClosingOrder(TradeOperation opr) {
		return opr.toString().charAt(1) == 'P';
	}
	
	public static PositionDirectionEnum getClosingDirection(DirectionEnum dir) {
		if(dir == DirectionEnum.D_Buy) {
			return PositionDirectionEnum.PD_Short;
		} else if(dir == DirectionEnum.D_Sell) {
			return PositionDirectionEnum.PD_Long;
		} 
		
		throw new IllegalArgumentException("无法确定[" + dir + "]的对应持仓方向");
	}

}
