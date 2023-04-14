package org.dromara.northstar.main.service;

import java.util.concurrent.ConcurrentMap;

import org.dromara.northstar.domain.account.TradeDayAccount;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.exception.InsufficientException;
import tech.quantit.northstar.common.exception.NoSuchElementException;
import tech.quantit.northstar.common.exception.TradeException;
import tech.quantit.northstar.common.model.OrderRecall;
import tech.quantit.northstar.common.model.OrderRequest;
import tech.quantit.northstar.common.utils.OrderUtils;

/**
 * 账户服务
 * @author KevinHuangwl
 *
 */
@Slf4j
public class AccountService {
	
	protected ConcurrentMap<String, TradeDayAccount> accountMap;
	
	public AccountService(ConcurrentMap<String, TradeDayAccount> accountMap) {
		this.accountMap = accountMap;
	}
	
	/**
	 * 下单
	 * @return
	 * @throws InsufficientException 
	 */
	public boolean submitOrder(OrderRequest req) throws InsufficientException {
		log.info("下单委托");
		String gatewayId = req.getGatewayId();
		TradeDayAccount account = accountMap.get(gatewayId);
		if(account == null) {
			throw new NoSuchElementException("没有找到该账户实例：" + gatewayId);
		}
		if(OrderUtils.isOpenningOrder(req.getTradeOpr())) {			
			account.openPosition(req);
		} else if(OrderUtils.isClosingOrder(req.getTradeOpr())) {
			account.closePosition(req);
		}
		
		return true;
	}
	
	/**
	 * 撤单
	 * @return
	 * @throws TradeException 
	 */
	public boolean cancelOrder(OrderRecall recall) throws TradeException {
		log.info("撤单委托");
		String gatewayId = recall.getGatewayId();
		TradeDayAccount account = accountMap.get(gatewayId);
		if(account == null) {
			throw new NoSuchElementException("没有找到该账户实例：" + gatewayId);
		}
		account.cancelOrder(recall);
		return true;
	}
	
}
