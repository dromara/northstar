package tech.xuanwu.northstar.strategy.config.strategy;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

import tech.xuanwu.northstar.strategy.trade.DataRef;

@ConfigurationProperties(prefix="strategy-setting.demo-strategy")
public class BaseStrategyConfig {

	/**
	 * 行情网关ID
	 */
	protected String gatewayId;
	
	/**
	 * 账户名称
	 */
	protected String accountId;
	
	/**
	 * 策略名称
	 */
	protected String strategyName;
	
	/**
	 * 行情合约与交易合约
	 */
	protected String[] mdtdContracts;
	
	/**
	 * 策略合约
	 */
	protected DataRef[] strategicContracts;
	
	/**
	 * tick回溯长度（单位：分钟）
	 */
	protected int tickRefLen;
	
	/**
	 * bar回溯长度（单位：分钟）
	 */
	protected int barRefLen;
	
	
	
	public void setGatewayId(String gatewayId) {
		this.gatewayId = gatewayId;
	}
	
	public String getGatewayId() {
		return gatewayId;
	}

	public String getAccountId() {
		return accountId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	public String getStrategyName() {
		return strategyName;
	}

	public void setStrategyName(String strategyName) {
		this.strategyName = strategyName;
	}

	public String[] getMdtdContracts() {
		return mdtdContracts;
	}

	public void setMdtdContracts(String[] mdtdContracts) {
		this.mdtdContracts = mdtdContracts;
		for(String contractPair : mdtdContracts) {
			String[] pair = StringUtils.split(contractPair, "@");
			if(pair.length != 2) {
				throw new IllegalStateException("合约设置有误。请确认策略配置中，mdtdContracts是以【行情合约@交易合约】的格式书写");
			}
			String mdContract = pair[0];
			String tdContract = pair[1];
			
			
		}
	}

	public int getTickRefLen() {
		return tickRefLen;
	}

	public void setTickRefLen(int tickRefLen) {
		this.tickRefLen = tickRefLen;
	}

	public int getBarRefLen() {
		return barRefLen;
	}

	public void setBarRefLen(int barRefLen) {
		this.barRefLen = barRefLen;
	}

}
