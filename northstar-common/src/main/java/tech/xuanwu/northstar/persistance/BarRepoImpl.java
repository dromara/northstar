package tech.xuanwu.northstar.persistance;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.mongodb.MongoClient;
import com.mongodb.client.AggregateIterable;

import tech.xuanwu.northstar.persistance.po.Bar;
import tech.xuanwu.northstar.utils.MongoDBClient;
import tech.xuanwu.northstar.utils.MongoDBUtils;

@Repository
public class BarRepoImpl implements BarRepo{
	
	@Autowired
	protected MongoDBClient client;
	
	@Autowired
	protected MongoClient client0;
	
	private static final String DB_BAR = "DB_BAR";

	@Override
	public void save(Bar bar) {
		client.insert(DB_BAR, bar.getUnifiedSymbol(), MongoDBUtils.beanToDocument(bar));
	}

	/**
	 * 查询当天的bar数据
	 */
	@Override
	public List<Bar> loadCurrentTradingDay(String unifiedSymbol) {
		LinkedList<String> refDays = getRefTradingDay(unifiedSymbol, 0);
		if(refDays.size() < 1) {
			throw new IllegalStateException("没有找到相关日期数据");
		}
		String currentTradingDay = refDays.poll();
		List<Document> resultList = client.find(DB_BAR, unifiedSymbol, new Document().append("tradingDay", currentTradingDay));
		
		return resultList.stream().map(doc -> MongoDBUtils.documentToBean(doc, Bar.class)).collect(Collectors.toList());
	}

	/**
	 * 查询近N天的bar数据
	 */
	@Override
	public List<Bar> loadNDaysRef(String unifiedSymbol, int days) {
		LinkedList<String> refDays = getRefTradingDay(unifiedSymbol, days);
		String earliestDay = refDays.pollLast();
		List<Document> resultList = client.find(DB_BAR, unifiedSymbol, 
				new Document().append("tradingDay", new Document().append("$gte", earliestDay)));
		return resultList.stream().map(doc -> MongoDBUtils.documentToBean(doc, Bar.class)).collect(Collectors.toList());
	}
	
	//查询合约的回溯日期
	private LinkedList<String> getRefTradingDay(String unifiedSymbol, int ref) {
		AggregateIterable<Document> result = client0.getDatabase(DB_BAR).getCollection(unifiedSymbol).aggregate(
				Arrays.asList(
						new Document().append("$group", new Document().append("_id", "$tradingDay")),
						new Document().append("$sort", new Document().append("_id", -1)),
						new Document().append("$limit", ref + 1)));
		Iterator<Document> itDoc = result.iterator();
		LinkedList<String> resultList = new LinkedList<>();
		while(itDoc.hasNext()) {
			Document document = itDoc.next();
			resultList.add(document.getString("_id"));
		}
		return resultList;
	}

}
