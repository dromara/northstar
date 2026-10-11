package org.dromara.northstar.common.constant;

import static xyz.redtorch.pb.CoreEnum.DirectionEnum.*;
import static xyz.redtorch.pb.CoreEnum.OffsetFlagEnum.*;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;

public enum TradeOperation {

	/**
	 * 买开
	 */
	BK(D_Buy, OF_Open),
	/**
	 * 卖开
	 */
	SK(D_Sell, OF_Open),
	/**
	 * 买平
	 */
	BP(D_Buy, OF_Close),
	/**
	 * 卖平
	 */
	SP(D_Sell, OF_Close);
	
	private DirectionEnum dir;
	private OffsetFlagEnum flag;
	
	private TradeOperation(DirectionEnum dir, OffsetFlagEnum flag) {
		this.dir = dir;
		this.flag = flag;
	}
	
	public DirectionEnum getDirection() {
		return dir;
	}
	
	public OffsetFlagEnum getOffsetFlag() {
		return flag;
	}
	
}
