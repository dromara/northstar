package tech.quantit.northstar.data;

import java.time.LocalDate;
import java.util.List;

import tech.quantit.northstar.common.constant.GatewayType;
import xyz.redtorch.pb.CoreField.BarField;

/**
 * 行情数据持久化
 * @author KevinHuangwl
 *
 */
public interface IMarketDataRepository {

	/**
	 * 移除网关行情数据
	 * @param gatewayId
	 */
	void dropGatewayData(String gatewayId);
	
	/**
	 * 保存数据
	 * @param bar
	 */
	void insert(BarField bar);
	
	/**
	 * 加载历史行情K线数据
	 * @param gatewayId
	 * @param unifiedSymbol
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	List<BarField> loadBars(String gatewayId, String unifiedSymbol, LocalDate startDate, LocalDate endDate);
	
	/**
	 * 查询某年的法定节假日（即不包含周末的非交易日）
	 * @param gatewayType
	 * @param year
	 * @return
	 */
	List<LocalDate> findHodidayInLaw(GatewayType gatewayType, int year);
}
