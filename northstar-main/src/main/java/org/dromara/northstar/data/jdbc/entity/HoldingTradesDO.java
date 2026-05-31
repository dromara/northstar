package org.dromara.northstar.data.jdbc.entity;

import java.util.List;

import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.gateway.mktdata.MarketCenter;

import com.alibaba.fastjson2.JSON;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name="HOLDINGS", indexes = {
		@Index(name="idx_holdings_account_name", columnList = "accountName"),
})
public class HoldingTradesDO {

	@Id
	private String id;
	
	private String accountName;
	
	private String contractId;
	
	private PositionDirectionEnum positionDir;
	
	@Lob
	private String dataStr;
	
	public List<Trade> convertTo(MarketCenter mktCenter){
		Contract contract = mktCenter.getContract(Identifier.of(contractId)).contract();
		return JSON.parseArray(dataStr, Trade.class).stream()
				.map(Trade::toBuilder)
				.map(trb -> trb.contract(contract).build())
				.toList();
	}
}
