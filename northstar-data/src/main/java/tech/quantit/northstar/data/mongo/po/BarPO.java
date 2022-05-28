package tech.quantit.northstar.data.mongo.po;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import xyz.redtorch.pb.CoreField.BarField;

/**
 * 每分钟K线
 * @author KevinHuangwl
 *
 */
@Data
@Document
public class BarPO {

	@Id
	private String id;
	
	private String unifiedSymbol;
	
	private String tradingDay;
	
	private long actionTime;
	
	private byte[] data;
	
	public static BarPO convertFrom(BarField bar) {
		BarPO po = new BarPO();
		po.id = bar.getUnifiedSymbol() + "_" + bar.getActionDay() + "_" + bar.getActionTime();
		po.unifiedSymbol = bar.getUnifiedSymbol();
		po.tradingDay = bar.getTradingDay();
		po.actionTime = bar.getActionTimestamp();
		po.data = bar.toByteArray();
		return po;
	}
}
