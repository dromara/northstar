package tech.quantit.northstar.data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 行情数据持久化
 * @author KevinHuangwl
 *
 */
public interface IMarketDataRepository {

	/**
	 * 初始化
	 * @param gatewayId
	 */
	void init(String gatewayId);

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
	 * 批量保存TICK数据
	 * @param barList
	 */
	void insertTicks(List<TickField> tickList);
	
	/**
	 * 按天加载BAR数据（方便缓存结果）
	 * @param gatewayId
	 * @param unifiedSymbol
	 * @param tradeDay
	 * @return
	 */
	List<BarField> loadBarsByDate(String gatewayId, String unifiedSymbol, LocalDate tradeDay);
	
	/**
	 * 按分钟加载TICK数据
	 * @param gatewayId
	 * @param unifiedSymbol
	 * @param dateTime
	 * @return
	 */
	List<TickField> loadTicksByDateTime(String gatewayId, String unifiedSymbol, LocalDateTime dateTime);
	
	/**
	 * 查询可用交易日
	 * @param gatewayId
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	List<LocalDate> findAvailableTradeDates(String gatewayId, LocalDate startDate, LocalDate endDate);
}
