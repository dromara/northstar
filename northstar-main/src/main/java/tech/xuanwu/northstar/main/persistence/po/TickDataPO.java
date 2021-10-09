package tech.xuanwu.northstar.main.persistence.po;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TickDataPO {

	private String actionTime ;  // 时间(HHmmssSSS)
	private double lastPrice ;  // 最新成交价
	private long volumeDelta ;  // 成交量变化
	private double turnoverDelta ;  // 成交总额变化
	private long numTradesDelta ;  // 成交笔数
	private double openInterestDelta ;  // 持仓量变化
}
