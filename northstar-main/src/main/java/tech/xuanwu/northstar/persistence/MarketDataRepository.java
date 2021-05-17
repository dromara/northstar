package tech.xuanwu.northstar.persistence;

import java.util.List;
import java.util.stream.Collectors;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import tech.xuanwu.northstar.persistence.po.MinBarDataPO;
import tech.xuanwu.northstar.utils.MongoClientAdapter;
import tech.xuanwu.northstar.utils.MongoUtils;

/**
 * MongoDB中行情数据的存储结构为：
 * Database: gateway
 * Collection: unifiedSymbol
 * Document: min-bar
 * 
 * @author KevinHuangwl
 */
@Repository
public class MarketDataRepository {

	@Autowired
	private MongoClientAdapter client;
	
	private static final String DB_PREFIX = "DB_DATA_";
	
	/**
	 * 保存数据
	 * @param bar
	 */
	public void insert(MinBarDataPO bar) {
		String collection = bar.getUnifiedSymbol();
		String db = DB_PREFIX + bar.getGatewayId();
		client.insert(db, collection, MongoUtils.beanToDocument(bar));
	}
	
	/**
	 * 按天加载数据（方便缓存结果）
	 * @param gatewayId
	 * @param unifiedSymbol
	 * @param tradeDay
	 * @return
	 */
	public List<MinBarDataPO> loadDataByDate(String gatewayId, String unifiedSymbol, String tradeDay) {
		String db = DB_PREFIX + gatewayId;
		String collection = unifiedSymbol;
		List<Document> resultList = client.find(db, collection, new Document().append("tradingDay", tradeDay));
		return resultList.stream().map(doc -> MongoUtils.documentToBean(doc, MinBarDataPO.class)).collect(Collectors.toList());
	}
	
}
