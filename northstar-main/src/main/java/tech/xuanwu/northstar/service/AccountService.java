package tech.xuanwu.northstar.service;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.event.InternalEventBus;
import tech.xuanwu.northstar.common.exception.InsufficientException;
import tech.xuanwu.northstar.common.exception.NoSuchElementException;
import tech.xuanwu.northstar.common.exception.TradeException;
import tech.xuanwu.northstar.common.model.OrderRecall;
import tech.xuanwu.northstar.common.model.OrderRequest;
import tech.xuanwu.northstar.common.utils.OrderUtil;
import tech.xuanwu.northstar.domain.TradeDayAccount;
import tech.xuanwu.northstar.factories.TradeDayAccountFactory;
import tech.xuanwu.northstar.handler.AccountEventHandler;
import tech.xuanwu.northstar.handler.ContractEventHandler;
import tech.xuanwu.northstar.model.ContractManager;

/**
 * 账户服务
 * @author KevinHuangwl
 *
 */
@Slf4j
@Service
public class AccountService extends BaseService implements InitializingBean{
	
	@Autowired
	protected InternalEventBus eventBus;
	
	@Autowired
	protected ContractManager contractMgr;
	
	protected ConcurrentHashMap<String, TradeDayAccount> accountMap = new ConcurrentHashMap<>();
	
	/**
	 * 下单
	 * @return
	 * @throws InsufficientException 
	 */
	public boolean submitOrder(OrderRequest req) throws InsufficientException {
		log.info("用户：[{}]，下单委托", getUserName());
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
		log.info("用户：[{}]，撤单委托", getUserName());
		String gatewayId = recall.getGatewayId();
		TradeDayAccount account = accountMap.get(gatewayId);
		if(account == null) {
			throw new NoSuchElementException("没有找到该账户实例：" + gatewayId);
		}
		account.cancelOrder(recall);
		return true;
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		eventBus.register(new ContractEventHandler(contractMgr));
		eventBus.register(new AccountEventHandler(accountMap, new TradeDayAccountFactory(eventBus, contractMgr)));
	}
}
