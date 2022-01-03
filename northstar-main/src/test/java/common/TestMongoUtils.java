package common;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

public class TestMongoUtils {
	
	public static MongoClient client = MongoClients.create("mongodb://127.0.0.1:27017/TEST_NS_DB");
	
	public static void clearDB() {
		client.getDatabase("TEST_NS_DB").drop();
	}

}
