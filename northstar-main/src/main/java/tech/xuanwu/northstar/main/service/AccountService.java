package tech.xuanwu.northstar.main.service;

import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.exception.InsufficientException;
import tech.xuanwu.northstar.common.exception.NoSuchElementException;
import tech.xuanwu.northstar.common.exception.TradeException;
import tech.xuanwu.northstar.common.model.OrderRecall;
import tech.xuanwu.northstar.common.model.OrderRequest;
import tech.xuanwu.northstar.common.utils.OrderUtil;
import tech.xuanwu.northstar.domain.account.TradeDayAccount;

/**
 * 账户服务
 * @author KevinHuangwl
 *
 */
@Slf4j
public class AccountService {
	
	protected ConcurrentHashMap<String, TradeDayAccount> accountMap;
	
	public AccountService(ConcurrentHashMap<String, TradeDayAccount> accountMap) {
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
		if(OrderUtil.isOpenningOrder(req.getTradeOpr())) {			
			account.openPosition(req);
		} else if(OrderUtil.isClosingOrder(req.getTradeOpr())) {
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
