package org.dromara.northstar.common.model;

import java.util.Objects;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.model.core.Contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContractSimpleInfo {
	
	/**
	 * 合约ID
	 */
	private String id;
	/**
	 * 合约名
	 */
	private String name;
	/**
	 * 合约英文名
	 */
	private String symbol;
	/**
	 * 合约代码
	 */
	private String unifiedSymbol;
	/**
	 * 渠道类型
	 */
	private ChannelType channelType;
	/**
	 * 合约价格精度
	 */
	private int precision;
	/**
	 * 到期日
	 */
	private String dueDate;
	
	public static ContractSimpleInfo from(Contract c) {
		return ContractSimpleInfo.builder()
				.id(c.contractId())
				.name(c.name())
				.symbol(c.symbol())
				.unifiedSymbol(c.unifiedSymbol())
				.channelType(c.channelType())
				.precision(c.pricePrecision())
				.dueDate(c.lastTradeDate().format(DateTimeConstant.D_FORMAT_FORMATTER))
				.build();
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(channelType, id, name, precision, unifiedSymbol);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ContractSimpleInfo other = (ContractSimpleInfo) obj;
		return channelType == other.channelType && Objects.equals(id, other.id)
				&& Objects.equals(name, other.name) && precision == other.precision
				&& Objects.equals(unifiedSymbol, other.unifiedSymbol);
	}
	
}
