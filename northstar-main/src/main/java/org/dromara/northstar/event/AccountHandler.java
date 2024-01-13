package org.dromara.northstar.event;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import org.dromara.northstar.account.AccountManager;
import org.dromara.northstar.account.TradeAccount;
import org.dromara.northstar.common.event.AbstractEventHandler;
import org.dromara.northstar.common.event.GenericEventHandler;
import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.exception.NoSuchElementException;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.core.Account;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Position;
import org.dromara.northstar.common.model.core.Trade;

/**
 * 处理账户相关操作
 * @author KevinHuangwl
 *
 */
public class AccountHandler extends AbstractEventHandler implements GenericEventHandler{

	private AccountManager accountMgr;
	
	private static final Set<NorthstarEventType> TARGET_TYPE = EnumSet.of(
			NorthstarEventType.ACCOUNT,
			NorthstarEventType.POSITION,
			NorthstarEventType.TRADE,
			NorthstarEventType.ORDER
	); 
			
	public AccountHandler(AccountManager accountMgr) {
		this.accountMgr = accountMgr;
	}
	
	@Override
	public synchronized void doHandle(NorthstarEvent e) {
		switch(e.getEvent()) {
		case ACCOUNT -> {
			Account af = (Account) e.getData();
			getAccount(af.gatewayId()).onAccount(af);
		}
		case POSITION -> {
			Position pf = (Position) e.getData();
			getAccount(pf.gatewayId()).onPosition(pf);
		}
		case TRADE -> {
			Trade tf = (Trade) e.getData();
			getAccount(tf.gatewayId()).onTrade(tf);
		}
		case ORDER -> {
			Order of = (Order) e.getData();
			getAccount(of.gatewayId()).onOrder(of);
		}
		default -> throw new IllegalArgumentException("Unexpected value: " + e.getEvent());
		}
	}
	
	private TradeAccount getAccount(String gatewayId) {
		TradeAccount account = accountMgr.get(Identifier.of(gatewayId));
		if(Objects.isNull(account)) {
			throw new NoSuchElementException("找不到账户：" + gatewayId);
		}
		return account;
	}
	
	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return TARGET_TYPE.contains(eventType);
	}
	
}
