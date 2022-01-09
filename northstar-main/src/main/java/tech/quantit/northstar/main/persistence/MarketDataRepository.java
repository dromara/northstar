package tech.quantit.northstar.main.persistence;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.main.persistence.po.ContractPO;
import tech.quantit.northstar.main.persistence.po.MinBarDataPO;
import tech.quantit.northstar.main.utils.MongoUtils;

/**
 * MongoDB中行情数据的存储结构为：
 * Database: gateway
 * Collection: unifiedSymbol
 * Document: min-bar
 * 
 * @author KevinHuangwl
 */
@Slf4j
@Repository
public class MarketDataRepository {

	@Autowired
	private MongoClientAdapter client;
	
	@Autowired
	private MongoTemplate mongo;
	
	private String dbName;
	private static final String COLLECTION_PREFIX = "DATA_";

	@PostConstruct
	protected void init() {
		dbName = mongo.getDb().getName();
	}
	
	private static final String SYMBOL = "unifiedSymbol";
	private static final String TRADING_DAY = "tradingDay";
	
	/**
	 * 初始化表
	 * @param gatewayId
	 */
	public void init(String gatewayId) {
		String collectionName = COLLECTION_PREFIX + gatewayId;
		if(mongo.collectionExists(collectionName)) {
			return;
		}
		log.debug("初始化表：{}", collectionName);
		mongo.createCollection(collectionName);
		IndexDefinition indexDefinition = new CompoundIndexDefinition(new Document().append(SYMBOL, 1).append(TRADING_DAY, 1));
		mongo.indexOps(collectionName).ensureIndex(indexDefinition);
		IndexDefinition indexDefinition2 = new CompoundIndexDefinition(new Document().append(SYMBOL, 1));
		mongo.indexOps(collectionName).ensureIndex(indexDefinition2);
		IndexDefinition indexDefinition3 = new CompoundIndexDefinition(new Document().append(TRADING_DAY, 1));
		mongo.indexOps(collectionName).ensureIndex(indexDefinition3);
	}

	/**
	 * 移除行情表
	 * @param gatewayId
	 */
	public void dropGatewayData(String gatewayId) {
		String collectionName = COLLECTION_PREFIX + gatewayId;
		if(mongo.collectionExists(collectionName)) {
			mongo.dropCollection(collectionName);
		}
	}
	
	/**
	 * 保存数据
	 * @param bar
	 */
	public void insert(MinBarDataPO bar) {
		log.debug("保存Bar数据：{}", bar.getUnifiedSymbol());
		client.insert(dbName, COLLECTION_PREFIX + bar.getGatewayId(), MongoUtils.beanToDocument(bar));
	}
	
	/**
	 * 批量保存数据
	 * @param barList
	 */
	public void insertMany(List<MinBarDataPO> barList) {
		log.debug("批量保存Bar数据：{}条", barList.size());
		List<Document> data = barList.stream()
				.map(MongoUtils::beanToDocument)
				.toList();
		client.insertMany(dbName, COLLECTION_PREFIX + barList.get(0).getGatewayId(), data);
	}
	
	/**
	 * 按天加载数据（方便缓存结果）
	 * @param gatewayId
	 * @param unifiedSymbol
	 * @param tradeDay
	 * @return
	 */
	public List<MinBarDataPO> loadDataByDate(String gatewayId, String unifiedSymbol, String tradeDay) {
		List<Document> resultList = client.find(dbName, COLLECTION_PREFIX + gatewayId, new Document()
				.append(SYMBOL, unifiedSymbol)
				.append(TRADING_DAY, tradeDay));;
		log.debug("[{}]-[{}]-[{}] 加载历史数据：{}条", gatewayId, unifiedSymbol, tradeDay, resultList.size());
		return resultList.stream()
				.map(doc -> MongoUtils.documentToBean(doc, MinBarDataPO.class))
				.sorted((a, b) -> a.getUpdateTime() < b.getUpdateTime() ? -1 : 1)
				.toList();
	}
	
	/**
	 * 查询行情数据可用日期
	 * @param gatewayId
	 * @param unifiedSymbol
	 * @return
	 */
	public List<String> findDataAvailableDates(String gatewayId, String unifiedSymbol, boolean isAsc){
		Bson filter = new Document().append("$match", new Document().append(SYMBOL, unifiedSymbol));
		Bson aggregator = new Document().append("$group", new Document().append("_id", "$tradingDay"));
		Bson sorter = new Document().append("$sort", new Document().append("_id", isAsc ? 1 : -1));
		List<Document> resultList = client.aggregate(dbName, COLLECTION_PREFIX + gatewayId, List.of(filter, aggregator, sorter));
		return resultList.stream().map(doc -> doc.get("_id").toString()).toList();
	}
	
	/**
	 * 批量保存合约信息
	 * @param contracts
	 */
	public void batchSaveContracts(List<ContractPO> contracts) {
		if(contracts.isEmpty()) {
			return;
		}
		log.debug("保存合约：{}条", contracts.size());
		long start = System.currentTimeMillis();
		for(ContractPO po : contracts) {
			mongo.save(po);
		}
		log.debug("合约保存成功，耗时{}毫秒", System.currentTimeMillis() - start);
	}
	
	/**
	 * 保存合约信息
	 * @param contract
	 */
	public void saveContract(ContractPO contract) {
		mongo.save(contract);
	}

	/**
	 * 清理特定时间的行情
	 * @param startTime
	 * @param endTime
	 */
	public void clearDataByTime(String gatewayId, long startTime, long endTime) {
		mongo.remove(Query.query(Criteria.where("actionTimestamp").gte(startTime).lte(endTime)), COLLECTION_PREFIX + gatewayId);
	}

	private static final long DAY14MILLISEC = TimeUnit.DAYS.toMillis(14);
	
	/**
	 * 查询有效合约列表
	 * @return
	 */
	public List<ContractPO> getAvailableContracts(){
		log.debug("查询十四天内登记过的有效合约");
		long day14Ago = System.currentTimeMillis() - DAY14MILLISEC;
		return mongo.find(Query.query(Criteria.where("updateTime").gt(day14Ago)), ContractPO.class);
	}
}
