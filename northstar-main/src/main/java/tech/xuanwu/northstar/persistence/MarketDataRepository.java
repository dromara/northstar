package tech.xuanwu.northstar.persistence;

import java.util.List;
import java.util.stream.Collectors;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.index.IndexDefinition;
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
	
	@Autowired
	private MongoTemplate mongo;
	
	private static final String DB = "NS_DB";
	private static final String COLLECTION_PREFIX = "DATA_";
	
	/**
	 * 初始化表
	 * @param gatewayId
	 */
	public void init(String gatewayId) {
		String collectionName = COLLECTION_PREFIX + gatewayId;
		if(mongo.collectionExists(collectionName)) {
			return;
		}
		mongo.createCollection(collectionName);
		IndexDefinition indexDefinition = new CompoundIndexDefinition(new Document().append("unifiedSymbol", 1).append("tradingDay", 1));
		mongo.indexOps(collectionName).ensureIndex(indexDefinition);
		
	}
	
	/**
	 * 保存数据
	 * @param bar
	 */
	public void insert(MinBarDataPO bar) {
		client.insert(DB, COLLECTION_PREFIX + bar.getGatewayId(), MongoUtils.beanToDocument(bar));
	}
	
	/**
	 * 批量保存数据
	 * @param barList
	 */
	public void insertMany(List<MinBarDataPO> barList) {
		List<Document> data = barList.stream()
				.map(bar -> MongoUtils.beanToDocument(bar))
				.collect(Collectors.toList());
		client.insertMany(DB, COLLECTION_PREFIX + barList.get(0).getGatewayId(), data);
	}
	
	/**
	 * 按天加载数据（方便缓存结果）
	 * @param gatewayId
	 * @param unifiedSymbol
	 * @param tradeDay
	 * @return
	 */
	public List<MinBarDataPO> loadDataByDate(String gatewayId, String unifiedSymbol, String tradeDay) {
		List<Document> resultList = client.find(DB, COLLECTION_PREFIX + gatewayId, new Document()
				.append("unifiedSymbol", unifiedSymbol)
				.append("tradingDay", tradeDay));
		return resultList.stream().map(doc -> MongoUtils.documentToBean(doc, MinBarDataPO.class)).collect(Collectors.toList());
	}

}
