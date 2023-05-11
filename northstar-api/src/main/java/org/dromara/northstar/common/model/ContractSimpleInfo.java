package org.dromara.northstar.common.model;

import java.util.Objects;

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
	 * 合约名
	 */
	private String name;
	/**
	 * 合约代码
	 */
	private String unifiedSymbol;
	/**
	 * 合约ID
	 */
	private String value;
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ContractSimpleInfo other = (ContractSimpleInfo) obj;
		return Objects.equals(value, other.value);
	}
	@Override
	public int hashCode() {
		return Objects.hash(value);
	}
	
}
