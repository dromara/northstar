package tech.xuanwu.northstar.persistence;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.persistence.po.ContractPO;
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
@Slf4j
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
		log.info("初始化表：{}", collectionName);
		mongo.createCollection(collectionName);
		IndexDefinition indexDefinition = new CompoundIndexDefinition(new Document().append("unifiedSymbol", 1).append("tradingDay", 1));
		mongo.indexOps(collectionName).ensureIndex(indexDefinition);
		
	}
	
	/**
	 * 保存数据
	 * @param bar
	 */
	public void insert(MinBarDataPO bar) {
		log.info("保存Bar数据：{}", bar.getUnifiedSymbol());
		client.insert(DB, COLLECTION_PREFIX + bar.getGatewayId(), MongoUtils.beanToDocument(bar));
	}
	
	/**
	 * 批量保存数据
	 * @param barList
	 */
	public void insertMany(List<MinBarDataPO> barList) {
		log.info("批量保存Bar数据：{}条", barList.size());
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
		log.info("[{}]-[{}]-[{}] 加载历史数据：{}条", gatewayId, unifiedSymbol, tradeDay, resultList.size());
		return resultList.stream().map(doc -> MongoUtils.documentToBean(doc, MinBarDataPO.class)).collect(Collectors.toList());
	}
	
	/**
	 * 批量保存合约信息
	 * @param contracts
	 */
	public void batchSaveContracts(List<ContractPO> contracts) {
		if(contracts.size() < 1) {
			return;
		}
		log.info("网关-[{}] 保存合约：{}条", contracts.get(0).getGatewayId(), contracts.size());
		long start = System.currentTimeMillis();
		for(ContractPO po : contracts) {
			mongo.save(po);
		}
		log.info("合约保存成功，耗时{}毫秒", System.currentTimeMillis() - start);
	}

	private static final long DAY14MILLISEC = TimeUnit.DAYS.toMillis(14);
	
	/**
	 * 查询有效合约列表
	 * @return
	 */
	public List<ContractPO> getAvailableContracts(){
		// 查询十四天内的数据集
		long day14Ago = System.currentTimeMillis() - DAY14MILLISEC;
		return mongo.find(Query.query(Criteria.where("recordTimestamp").gt(day14Ago)), ContractPO.class);
	}
}
