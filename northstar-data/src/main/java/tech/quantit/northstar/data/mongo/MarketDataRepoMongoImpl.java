package tech.quantit.northstar.data.mongo;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.data.IMarketDataRepository;
import tech.quantit.northstar.data.mongo.po.BarPO;
import tech.quantit.northstar.data.mongo.po.MinTicksPO;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 行情服务
 * @author : wpxs
 */
@Slf4j
public class MarketDataRepoMongoImpl implements IMarketDataRepository {

	private MongoTemplate mongoTemplate;

	public MarketDataRepoMongoImpl(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	/**
	 * 初始化表
	 * @param gatewayId
	 */
	@Override
	public void init(String gatewayId) {
		// TODO Auto-generated method stub

	}

	/**
	 * 移除网关行情数据
	 * @param gatewayId
	 */
	@Override
	public void dropGatewayData(String gatewayId) {
		// TODO Auto-generated method stub

	}

	/**
	 * 保存数据
	 * @param bar
	 */
	@Override
	public void insert(BarField bar) {
		mongoTemplate.insert(BarPO.convertFrom(bar));
	}

	/**
	 * 批量保存TICK数据
	 * @param tickList
	 */
	@Override
	public void insertTicks(List<TickField> tickList) {
		mongoTemplate.insert(MinTicksPO.convertFrom(tickList));
	}

	/**
	 * 按天加载BAR数据（方便缓存结果）
	 * @param gatewayId
	 * @param unifiedSymbol
	 * @param tradeDay
	 * @return
	 */
	@Override
	public List<BarField> loadBarsByDate(String gatewayId, String unifiedSymbol, LocalDate tradeDay) {
		String tradingDay = tradeDay.format(DateTimeConstant.D_FORMAT_INT_FORMATTER);
		Query query = Query.query(Criteria.where("unifiedSymbol").is(unifiedSymbol).and("tradingDay").is(tradingDay));
		List<BarPO> barList = mongoTemplate.find(query, BarPO.class);
		return barList.stream().map(this::convertBar).filter(Objects::nonNull).filter(bar -> gatewayId.equals(bar.getGatewayId())).collect(Collectors.toList());
	}

	/**
	 * 按分钟加载TICK数据
	 * @param gatewayId
	 * @param unifiedSymbol
	 * @param dateTime
	 * @return
	 */
	@Override
	public List<TickField> loadTicksByDateTime(String gatewayId, String unifiedSymbol, LocalDateTime dateTime) {
		String tradingDay = dateTime.format(DateTimeConstant.D_FORMAT_INT_FORMATTER);
		Query query = Query.query(Criteria.where("unifiedSymbol").is(unifiedSymbol).and("tradingDay").is(tradingDay));
		MinTicksPO minTicks = mongoTemplate.findOne(query, MinTicksPO.class);
		if (minTicks == null) {
			return new ArrayList<>();
		}
		return this.convertTick(minTicks).stream().filter(Objects::nonNull).filter(tick -> gatewayId.equals(tick.getGatewayId())).collect(Collectors.toList());
	}

	/**
	 * 查询可用交易日
	 * @param gatewayId
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	@Override
	public List<LocalDate> findAvailableTradeDates(String gatewayId, LocalDate startDate, LocalDate endDate) {
		// TODO Auto-generated method stub
		return null;
	}

	private BarField convertBar(BarPO po) {
		try {
			return BarField.parseFrom(po.getData());
		} catch (InvalidProtocolBufferException e) {
			log.warn("", e);
			return null;
		}
	}

	private List<TickField> convertTick(MinTicksPO po) {
		return po.getData().stream().map(by -> {
			try {
				return TickField.parseFrom(by);
			} catch (InvalidProtocolBufferException e) {
				log.warn("", e);
				return null;
			}
		}).collect(Collectors.toList());
	}
}
