package tech.xuanwu.northstar.persistance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.bson.Document;
import org.junit.Before;
import org.junit.Test;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.AggregateIterable;
import com.mongodb.operation.AggregateOperation;

import tech.xuanwu.northstar.persistance.BarRepo;
import tech.xuanwu.northstar.persistance.BarRepoImpl;
import tech.xuanwu.northstar.persistance.po.Bar;
import tech.xuanwu.northstar.utils.MongoDBClient;
import xyz.redtorch.pb.CoreField.BarField;

public class BarRepoTest {

	private MongoClient client = new MongoClient(new ServerAddress("127.0.0.1", 27017));
	private MongoDBClient mongo = new MongoDBClient(client);
	
	private BarRepoImpl barRepo = new BarRepoImpl();
	
	@Before
	public void prepare() {
		barRepo.client = mongo;
		barRepo.client0 = client;
	}
	
	@Test
	public void testSave() {
		BarField bar = BarField.newBuilder()
				.setTradingDay("20191010")
				.setGatewayId("xysak")
				.setUnifiedSymbol("rb2010")
				.setActionTimestamp(System.currentTimeMillis())
				.setClosePrice(2.333333)
				.build();
		Bar barPO = Bar.convertFrom(bar);
		barRepo.save(barPO);
		List<Bar> resultList = barRepo.loadCurrentTradingDay("rb2010");
		assertThat(resultList.get(0)).isEqualTo(barPO);
	}
	
	@Test
	public void testLoadNDaysRef() {
		BarField bar = BarField.newBuilder()
				.setTradingDay("20191010")
				.setGatewayId("xysak")
				.setUnifiedSymbol("rb2010")
				.setActionTimestamp(System.currentTimeMillis())
				.setClosePrice(2.333333)
				.build();
		BarField bar2 = BarField.newBuilder()
				.setTradingDay("20191011")
				.setGatewayId("xysak")
				.setUnifiedSymbol("rb2010")
				.setActionTimestamp(System.currentTimeMillis())
				.setClosePrice(2.333333)
				.build();
		BarField bar3 = BarField.newBuilder()
				.setTradingDay("20191012")
				.setGatewayId("xysak")
				.setUnifiedSymbol("rb2010")
				.setActionTimestamp(System.currentTimeMillis())
				.setClosePrice(2.333333)
				.build();
		barRepo.save(Bar.convertFrom(bar));
		barRepo.save(Bar.convertFrom(bar2));
		barRepo.save(Bar.convertFrom(bar3));
		List<Bar> resultList = barRepo.loadNDaysRef("rb2010", 1);
		assertThat(resultList.size()).isEqualTo(2);
	}
}
