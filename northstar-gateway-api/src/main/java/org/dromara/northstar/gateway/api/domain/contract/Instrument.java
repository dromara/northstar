package org.dromara.northstar.gateway.api.domain.contract;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.gateway.api.domain.time.TradeTimeDefinition;

import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
import xyz.redtorch.pb.CoreField.ContractField;

/**
 * （可交易的）投资品种
 * @author KevinHuangwl
 *
 */
public interface Instrument {

	/**
	 * 名称
	 * @return
	 */
	String name();
	
	/**
	 * 唯一标识
	 * @return
	 */
	Identifier identifier();
	
	/**
	 * 种类
	 * @return
	 */
	ProductClassEnum productClass();
	
	/**
	 * 交易所
	 * @return
	 */
	ExchangeEnum exchange();
	
	/**
	 * 开市时间定义
	 * @return
	 */
	TradeTimeDefinition tradeTimeDefinition();
	
	/**
	 * 网关渠道类型
	 * @return
	 */
	ChannelType channelType();
	
	/**
	 * 设置合约定义
	 */
	default void setContractDefinition(ContractDefinition contractDef) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * 转换为合约信息
	 * @return
	 */
	default ContractField contractField() {
		throw new UnsupportedOperationException();
	}
}
