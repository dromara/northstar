package tech.xuanwu.northstar.persistance;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import tech.xuanwu.northstar.persistance.po.Account;
import tech.xuanwu.northstar.utils.MongoDBClient;
import tech.xuanwu.northstar.utils.MongoDBUtils;

@Repository
public class AccountRepoImpl implements AccountRepo{

	@Autowired
	protected MongoDBClient client;
	
	private static final String DB_ACCOUNT = "DB_ACCOUNT";
	
	@Override
	public Account findByGatewayId(String gatewayId) {
		List<Document> result = client.find(DB_ACCOUNT, gatewayId);
		if(result.size() == 0) {
			return null;
		}
		return MongoDBUtils.documentToBean(result.get(0), Account.class);
	}

	@Override
	public void save(Account account) {
		String gatewayId = account.getGatewayId();
		client.upsert(DB_ACCOUNT, gatewayId, MongoDBUtils.beanToDocument(account), new Document().append("accountId", account.getAccountId()));
	}

	@Override
	public List<Account> findAll() {
		List<String> collections = client.getAllCollections(DB_ACCOUNT);
		List<Account> result = new ArrayList<>();
		for(String gatewayId : collections) {
			Account account = findByGatewayId(gatewayId);
			result.add(account);
		}
		return result;
	}

}
