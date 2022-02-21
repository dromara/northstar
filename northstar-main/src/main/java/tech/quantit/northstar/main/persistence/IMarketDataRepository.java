package tech.quantit.northstar.main.persistence;

import java.util.List;

import tech.quantit.northstar.main.persistence.po.ContractPO;
import tech.quantit.northstar.main.persistence.po.MinBarDataPO;

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
	void insert(MinBarDataPO bar);
	
	/**
	 * 批量保存数据
	 * @param barList
	 */
	void insertMany(List<MinBarDataPO> barList);
	
	/**
	 * 按天加载数据（方便缓存结果）
	 * @param gatewayId
	 * @param unifiedSymbol
	 * @param tradeDay
	 * @return
	 */
	List<MinBarDataPO> loadDataByDate(String gatewayId, String unifiedSymbol, String tradeDay);
	
	/**
	 * 查询行情数据可用日期
	 * @param gatewayId
	 * @param unifiedSymbol
	 * @return
	 */
	List<String> findDataAvailableDates(String gatewayId, String unifiedSymbol, boolean isAsc);
	
	/**
	 * 批量保存合约信息
	 * @param contracts
	 */
	void batchSaveContracts(List<ContractPO> contracts);
	
	/**
	 * 保存合约信息
	 * @param contract
	 */
	void saveContract(ContractPO contract);

	/**
	 * 清理特定时间的行情
	 * @param startTime
	 * @param endTime
	 */
	void clearDataByTime(String gatewayId, long startTime, long endTime);

	/**
	 * 查询有效合约列表
	 * @return
	 */
	List<ContractPO> getAvailableContracts();
}
