package tech.quantit.northstar.data.mongo;

import com.mongodb.client.MongoClients;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.data.IMarketDataRepository;
import tech.quantit.northstar.data.mongo.po.BarPO;
import tech.quantit.northstar.data.mongo.po.MinTicksPO;
import xyz.redtorch.pb.CoreField;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 模拟账户服务测试
 * @author : wpxs
 */
public class MarketDataRepoMongoImplTest {

	MongoTemplate mongoTemplate = new MongoTemplate(MongoClients.create("mongodb://localhost:27017"), "TEST_NS_DB");

	IMarketDataRepository repo = new MarketDataRepoMongoImpl(mongoTemplate);

	CoreField.BarField barField1 = CoreField.BarField.newBuilder()
			.setUnifiedSymbol("rb2205@SHFE@FUTURES")
			.setGatewayId("test")
			.setTradingDay(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
			.setActionDay(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
			.setActionTime("101122")
			.setOpenPrice(1.0)
			.setHighPrice(2.0)
			.setLowPrice(0.5)
			.setClosePrice(1.5)
			.setOpenInterest(11.0)
			.setOpenInterestDelta(111.0)
			.setVolumeDelta(1112)
			.setTurnoverDelta(123)
			.setNumTradesDelta(12313)
			.build();

	CoreField.BarField barField2 = CoreField.BarField.newBuilder()
			.setUnifiedSymbol("rb2205@SHFE@FUTURES")
			.setGatewayId("test")
			.setTradingDay(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
			.setActionDay(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
			.setActionTime("102222")
			.setOpenPrice(1.0)
			.setHighPrice(2.0)
			.setLowPrice(0.5)
			.setClosePrice(1.5)
			.setOpenInterest(11.0)
			.setOpenInterestDelta(111.0)
			.setVolumeDelta(1112)
			.setTurnoverDelta(123)
			.setNumTradesDelta(12313)
			.build();

	CoreField.TickField  tickField1 = CoreField.TickField.newBuilder()
			.setUnifiedSymbol("rb2206@SHFE@FUTURES")
			.setGatewayId("test1")
			.setTradingDay(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
			.setActionDay(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
			.setActionTime("102233")
			.setOpenPrice(1.0)
			.setHighPrice(2.0)
			.setLowPrice(0.5)
			.setOpenInterest(11.0)
			.setOpenInterestDelta(111.0)
			.setVolumeDelta(1112)
			.setTurnoverDelta(123)
			.setNumTradesDelta(12313)
			.build();

	CoreField.TickField  tickField2 = CoreField.TickField.newBuilder()
			.setUnifiedSymbol("rb2206@SHFE@FUTURES")
			.setGatewayId("test1")
			.setTradingDay(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
			.setActionDay(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
			.setActionTime("103333")
			.setOpenPrice(1.0)
			.setHighPrice(2.0)
			.setLowPrice(0.5)
			.setOpenInterest(11.0)
			.setOpenInterestDelta(111.0)
			.setVolumeDelta(1112)
			.setTurnoverDelta(123)
			.setNumTradesDelta(12313)
			.build();

	CoreField.TickField  tickField3 = CoreField.TickField.newBuilder()
			.setUnifiedSymbol("rb2206@SHFE@FUTURES")
			.setGatewayId("test1")
			.setTradingDay(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
			.setActionDay(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
			.setActionTime("104433")
			.setOpenPrice(1.0)
			.setHighPrice(2.0)
			.setLowPrice(0.5)
			.setOpenInterest(11.0)
			.setOpenInterestDelta(111.0)
			.setVolumeDelta(1112)
			.setTurnoverDelta(123)
			.setNumTradesDelta(12313)
			.build();

	@AfterEach
	void clear() {
		mongoTemplate.dropCollection(BarPO.class);
		mongoTemplate.dropCollection(MinTicksPO.class);
	}

	@Test
	public void testInsert() {
		repo.insert(barField1);
		assertThat(mongoTemplate.findAll(BarPO.class)).hasSize(1);
	}

	@Test
	public void testInsertTicks() {
		repo.insertTicks(List.of(tickField1, tickField2, tickField3));
		assertThat(mongoTemplate.findAll(MinTicksPO.class)).hasSize(1);
	}

	@Test
	public void testLoadBarsByDate() {
		repo.insert(barField1);
		repo.insert(barField2);
		List<CoreField.BarField> test = repo.loadBarsByDate("test", "rb2205@SHFE@FUTURES", LocalDate.now());
		assertThat(test).hasSize(2);
	}

	@Test
	public void testLoadTicksByDateTime() {
		repo.insertTicks(List.of(tickField1, tickField2, tickField3));
		List<CoreField.TickField> test = repo.loadTicksByDateTime("test1", "rb2206@SHFE@FUTURES", LocalDateTime.now());
		assertThat(test).hasSize(3);
	}
}
