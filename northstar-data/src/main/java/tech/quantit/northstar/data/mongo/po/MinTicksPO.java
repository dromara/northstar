package tech.quantit.northstar.data.mongo.po;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 每分钟TICK数据
 * @author KevinHuangwl
 *
 */
@Data
@Document
public class MinTicksPO {

	@Id
	private String id;
	
	private String unifiedSymbol;
	
	private String tradingDay;
	
	private long actionTime;
	
	private List<byte[]> data;
	
	public static MinTicksPO convertFrom(List<TickField> ticks) {
		MinTicksPO po = new MinTicksPO();
		TickField lastTick = ticks.get(ticks.size() - 1);
		po.id = lastTick.getUnifiedSymbol() + "_" + lastTick.getActionDay() + "_" + lastTick.getActionTime();
		po.unifiedSymbol = lastTick.getUnifiedSymbol();
		po.tradingDay = lastTick.getTradingDay();
		po.actionTime = lastTick.getActionTimestamp();
		po.data = ticks.stream().map(TickField::toByteArray).toList();
		return po;
	}
}
