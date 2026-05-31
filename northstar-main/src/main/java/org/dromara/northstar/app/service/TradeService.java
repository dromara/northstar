package org.dromara.northstar.app.service;

import java.util.List;

import org.dromara.northstar.account.AccountManager;
import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.common.exception.NoConnectionException;
import org.dromara.northstar.common.exception.NoSuchGatewayException;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.LineTraderDTO;
import org.dromara.northstar.common.model.ModuleDealRecordVO;
import org.dromara.northstar.common.model.core.CancelOrderReq;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.data.IAccountRepository;
import org.dromara.northstar.event.PlannedTradeHandler;
import org.dromara.northstar.event.SseBroadcastHandler;
import org.dromara.northstar.gateway.Gateway;
import org.dromara.northstar.strategy.IAccount;
import org.dromara.northstar.trader.LineTrader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreField.TradeField;

@Slf4j
@Service
public class TradeService {
	
	@Autowired
	private SseBroadcastHandler broadcastHandler;
	
	@Autowired
	private PlannedTradeHandler plannedTradeHandler;
	
	@Autowired
	private IAccountRepository accountRepo;
	
	@Autowired
	private AccountManager accMgr;
	
	public void submitOrder(SubmitOrderReq orderReq) {
		log.info("手工交易发单");
		Identifier accountId = Identifier.of(orderReq.gatewayId());
		if(!accMgr.contains(accountId)) {
			throw new NoSuchGatewayException(orderReq.gatewayId());
		}
		IAccount account = accMgr.get(accountId);
		Gateway gateway = account.getTradeGateway();
		if(gateway.getConnection() != ConnectionState.CONNECTED) {
			throw new NoConnectionException();
		}
		account.submitOrder(orderReq);
	}
	
	public void cancelOrder(CancelOrderReq cancelReq) {
		log.info("手工交易撤单");
		Identifier accountId = Identifier.of(cancelReq.gatewayId());
		IAccount account = accMgr.get(accountId);
		Gateway gateway = account.getTradeGateway();
		if(gateway.getConnection() != ConnectionState.CONNECTED) {
			throw new NoConnectionException();
		}
		account.cancelOrder(cancelReq);
	}
	
	public void saveLineTrader(LineTraderDTO lineTraderDTO) {
		log.info("{} {} 保存画线交易计划{}：入场价[{}]，止盈[{}]，止损[{}]", 
				lineTraderDTO.getAccountGatewayId(), lineTraderDTO.getContractId(), lineTraderDTO.getId(),
				lineTraderDTO.getOrderPrice(), lineTraderDTO.getTakeProfitPrice(), lineTraderDTO.getStopLossPrice());
		plannedTradeHandler.addTrader(lineTraderDTO);
	}
	
	public List<LineTraderDTO> getLineTraders(String accountId, String contractId){
		return plannedTradeHandler.getTraders()
				.stream()
				.filter(dto -> dto.getAccountGatewayId().equals(accountId))
				.filter(dto -> contractId == null || dto.getContractId().equals(contractId))
				.toList();
	}
	
	public void removeLineTrader(String id) {
		LineTrader lineTrader = plannedTradeHandler.removeTrader(id);
		if(lineTrader != null) {			
			log.info("{} {} 移除画线交易计划", lineTrader.getDto().getAccountGatewayId(), lineTrader.getDto().getContractId());
		}
	}
	
	public void resendOrderAndTradeCache() {
		broadcastHandler.resendCache();
	}
	
	public List<ModuleDealRecordVO> getDealRecords(String accountId){
		return accountRepo.findDealRecords(accountId).stream()
				.map(mdr -> {
					TradeField openTrade = mdr.convertOpenTrade();
					TradeField closeTrade = mdr.convertCloseTrade();
					PositionDirectionEnum dir = switch(openTrade.getDirection()) {
						case D_Buy -> PositionDirectionEnum.PD_Long;
						case D_Sell -> PositionDirectionEnum.PD_Short;
						default -> throw new IllegalArgumentException();
					};
					return ModuleDealRecordVO.builder()
							.name(openTrade.getContract().getName())
							.symbol(openTrade.getContract().getSymbol())
							.contractId(openTrade.getContract().getContractId())
							.dir(dir)
							.vol(closeTrade.getVolume())
							.profit((int)mdr.getDealProfit())
							.transactionFee((int)mdr.getTransactionFee())
							.openPrice(String.format("%.2f", openTrade.getPrice()))
							.openTimestamp(openTrade.getTradeTimestamp())
							.closePrice(String.format("%.2f", closeTrade.getPrice()))
							.closeTimestamp(closeTrade.getTradeTimestamp())
							.build();
				})
				.toList();
	}
}
