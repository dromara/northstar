package tech.quantit.northstar.data.mongo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;

import tech.quantit.northstar.data.IMarketDataRepository;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

public class MarketDataRepoMongoImpl implements IMarketDataRepository {
	
	private MongoTemplate mongoTemplate;
	
	public MarketDataRepoMongoImpl(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	@Override
	public void init(String gatewayId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dropGatewayData(String gatewayId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void insert(BarField bar) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void insertTicks(List<TickField> tickList) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<BarField> loadBarsByDate(String gatewayId, String unifiedSymbol, LocalDate tradeDay) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<TickField> loadTicksByDateTime(String gatewayId, String unifiedSymbol, LocalDateTime dateTime) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<LocalDate> findAvailableTradeDates(String gatewayId, LocalDate startDate, LocalDate endDate) {
		// TODO Auto-generated method stub
		return null;
	}

}
