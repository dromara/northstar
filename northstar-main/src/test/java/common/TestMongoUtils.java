package common;

import com.mongodb.MongoClient;


public class TestMongoUtils {
	
	public static MongoClient client = new MongoClient("127.0.0.1", 27017);
	
	public static void clearDB() {
		client.dropDatabase("TEST_NS_DB");
	}

}
