package tech.quantit.northstar.strategy.api;

import java.util.List;

import tech.quantit.northstar.common.TickDataAware;
import tech.quantit.northstar.common.TransactionAware;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 
 * @author KevinHuangwl
 *
 */
public interface IModuleAccountStore extends TickDataAware, TransactionAware, ContextAware{
	/**
	 * 初始余额
	 * @return
	 */
	double getInitBalance(String gatewayId);
	/**
	 * 期初余额
	 * @return
	 */
	double getPreBalance(String gatewayId);
	/**
	 * 可用余额
	 * @param gatewayId
	 * @return
	 */
	double getAvailable(String gatewayId);
	/**
	 * 获取持仓信息
	 * @param gatewayId
	 * @return
	 */
	List<PositionField> getPositions(String gatewayId);
	/**
	 * 获取全部未平仓成交
	 * @return
	 */
	List<TradeField> getUncloseTrades(String gatewayId);
	/**
	 * 获取未平仓成交
	 * @param unifiedSymbol
	 * @param dir
	 * @return
	 */
	List<TradeField> getUncloseTrade(String gatewayId, String unifiedSymbol, DirectionEnum dir);
	/**
	 * 获取累计开平仓手数
	 * @return
	 */
	int getAccDealVolume(String gatewayId);
	/**
	 * 获取累计平仓盈亏
	 * @return
	 */
	double getAccCloseProfit(String gatewayId);
	/**
	 * 新交易日开盘前处理
	 */
	void tradeDayPreset();
	/**
	 * 获取模组状态机
	 * @return
	 */
	IModuleStateMachine getModuleStateMachine();
}
