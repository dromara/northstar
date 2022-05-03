package tech.quantit.northstar.common.utils;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreField.ContractField;

public class FieldUtils {

	private FieldUtils() {}
	
	public static String chn(DirectionEnum dir) {
		switch(dir) {
		case D_Buy:
			return "买";
		case D_Sell:
			return "卖";
		default:
			return "未知";
		}
	}
	
	public static String chn(OffsetFlagEnum offset) {
		switch (offset) {
		case OF_Open:
			return "开";
		case OF_Unknown:
			return "未知";
		case OF_Close:
			return "平";
		case OF_CloseToday:
			return "平今";
		case OF_CloseYesterday:
			return "平昨";
		default:
			return "强平";
		}
	}
	
	public static String chn(OrderStatusEnum status) {
		switch(status) {
		case OS_AllTraded:
			return "全成";
		case OS_Canceled:
			return "已撤单";
		case OS_Rejected:
			return "已拒绝";
		case OS_Touched:
		case OS_NoTradeQueueing:
			return "已挂单";
		case OS_NoTradeNotQueueing:
			return "未排队";
		case OS_PartTradedNotQueueing:
			return "部分未排队";
		case OS_PartTradedQueueing:
			return "部分成交";
		default:
			return "未知";
		}
	}
	
	public static boolean isLong(PositionDirectionEnum position) {
		return position == PositionDirectionEnum.PD_Long;
	}
	
	public static boolean isShort(PositionDirectionEnum position) {
		return position == PositionDirectionEnum.PD_Short;
	}
	
	public static boolean isBuy(DirectionEnum dir) {
		return dir == DirectionEnum.D_Buy;
	}
	
	public static boolean isSell(DirectionEnum dir) {
		return dir == DirectionEnum.D_Sell;
	}
	
	public static int directionFactor(DirectionEnum dir) {
		return switch(dir) {
		case D_Buy -> 1;
		case D_Sell -> -1;
		default -> 0;
		};
	}
	
	public static double marginRatio(ContractField contract, DirectionEnum dir) {
		return switch(dir) {
		case D_Buy -> contract.getLongMarginRatio();
		case D_Sell -> contract.getShortMarginRatio();
		default -> 0;
		};
	}
	
	public static boolean isOpposite(DirectionEnum dir1, DirectionEnum dir2) {
		return dir1 != dir2 && dir1 != DirectionEnum.D_Unknown && dir2 != DirectionEnum.D_Unknown;
	}
	
	public static boolean isOpen(OffsetFlagEnum offsetFlag) {
		return offsetFlag == OffsetFlagEnum.OF_Open;
	}
	
	public static boolean isClose(OffsetFlagEnum offsetFlag) {
		return offsetFlag != OffsetFlagEnum.OF_Unknown && offsetFlag != OffsetFlagEnum.OF_Open;
	}
}
