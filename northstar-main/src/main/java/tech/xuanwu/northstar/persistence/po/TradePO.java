package tech.xuanwu.northstar.persistence.po;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.PriceSourceEnum;
import xyz.redtorch.pb.CoreEnum.TradeTypeEnum;

@Document
@Data
public class TradePO {
	@Id
	private String tradeId;  // 成交ID,通常是<网关ID@定单编号@方向@成交编号>，加入方向是因为部分交易所发生违规自成交后,成交ID相同
	private String adapterTradeId;  // 适配器层成交ID
	private String originOrderId;  // 原始定单ID
	private String orderId;  // 定单ID,通常是<网关ID@定单ID>
	private String adapterOrderId;  // 适配器层定单ID
	private String orderLocalId; // 本地报单编号
	private String brokerOrderSeq; //经纪公司报单编号
	private String orderSysId; // 报单编号
	private String settlementId; // 结算编号
	private String sequenceNo; // 序列号
	private String accountId;  // 账户ID
	private DirectionEnum direction;  // 方向
	private OffsetFlagEnum offsetFlag;  // 开平
	private HedgeFlagEnum hedgeFlag; // 投机套保标识
	private double price;  // 价格
	private int volume;  // 数量
	private TradeTypeEnum tradeType; // 成交类型
	private PriceSourceEnum priceSource; // 成交价来源
	private String tradingDay;  // 交易日
	private String tradeDate;  // 成交日期
	private String tradeTime;  // 成交时间(HHmmssSSS)
	private long tradeTimestamp;  // 成交时间戳
	private String gatewayId;  // 网关ID
	private String moduleName; // 模组名称
}
