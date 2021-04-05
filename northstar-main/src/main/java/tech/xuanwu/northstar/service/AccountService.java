package tech.xuanwu.northstar.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.google.common.eventbus.Subscribe;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.constant.Constants;
import tech.xuanwu.northstar.common.event.InternalEventBus;
import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.common.exception.InsufficientException;
import tech.xuanwu.northstar.common.exception.NoSuchElementException;
import tech.xuanwu.northstar.common.exception.TradeException;
import tech.xuanwu.northstar.common.model.OrderRecall;
import tech.xuanwu.northstar.common.model.OrderRequest;
import tech.xuanwu.northstar.common.utils.OrderUtil;
import tech.xuanwu.northstar.domain.GatewayConnection;
import tech.xuanwu.northstar.domain.TradeDayAccount;
import xyz.redtorch.pb.CoreField.ContractField;

/**
 * 账户服务
 * @author KevinHuangwl
 *
 */
@Slf4j
@Service
public class AccountService {
	
	@Autowired
	protected InternalEventBus eventBus;
	
	@Autowired
	@Qualifier(Constants.GATEWAY_CONTRACT_MAP)
	protected Map<String, Map<String, ContractField>> gatewayContractMap;
	
	protected ConcurrentHashMap<String, TradeDayAccount> accountMap = new ConcurrentHashMap<>();

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
	
	@Subscribe
	private void onEvent(NorthstarEvent e) {
		if(e.getEvent() == NorthstarEventType.LOGINED) {
			onLogined(e);
		} else if (e.getEvent() == NorthstarEventType.DISCONNECTING) {
			onLogouted(e);
		}
	}
	
	public void onLogined(NorthstarEvent e) {
		String gatewayId = (String) e.getData();
		if(!gatewayContractMap.containsKey(gatewayId)) {
			throw new NoSuchElementException("没有找到网关的合约列表：" + gatewayId);
		}
		Map<String, ContractField> contractMap = gatewayContractMap.get(gatewayId);
		TradeDayAccount account = new TradeDayAccount(gatewayId, eventBus, contractMap);
		accountMap.put(gatewayId, account);
		log.info("账户登陆：{}", gatewayId);
	}
	
	public void onLogouted(NorthstarEvent e) {
		GatewayConnection conn = (GatewayConnection) e.getData();
		String gatewayId = conn.getGwDescription().getGatewayId();
		accountMap.remove(gatewayId);
		log.info("账户登出：{}", gatewayId);
	}
}
