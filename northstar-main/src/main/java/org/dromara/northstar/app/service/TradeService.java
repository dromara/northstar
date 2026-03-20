package org.dromara.northstar.app.service;

import java.util.List;
import java.util.UUID;

import org.dromara.northstar.account.AccountManager;
import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.common.exception.NoConnectionException;
import org.dromara.northstar.common.exception.NoSuchGatewayException;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.LineTraderDTO;
import org.dromara.northstar.common.model.ModuleDealRecordVO;
import org.dromara.northstar.common.model.OrderRequestDTO;
import org.dromara.northstar.common.model.OrderWithdrawDTO;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Position;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.utils.FieldUtils;
import org.dromara.northstar.data.jdbc.ModuleDealRecordRepository;
import org.dromara.northstar.data.jdbc.entity.ModuleDealRecordDO;
import org.dromara.northstar.event.PlannedTradeHandler;
import org.dromara.northstar.event.SseBroadcastHandler;
import org.dromara.northstar.gateway.Gateway;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.dromara.northstar.strategy.IAccount;
import org.dromara.northstar.trader.LineTrader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;
import xyz.redtorch.pb.CoreEnum.VolumeConditionEnum;
import xyz.redtorch.pb.CoreField.TradeField;

@Slf4j
@Service
public class TradeService {
	
	@Autowired
	private SseBroadcastHandler broadcastHandler;
	
	@Autowired
	private PlannedTradeHandler plannedTradeHandler;
	
	@Autowired
	private ModuleDealRecordRepository dealRepo;
	
	@Autowired
	private AccountManager accMgr;
	
	@Autowired
	private MarketCenter marketCenter;
	
	public void submitOrder(OrderRequestDTO dto) {
		log.info("手工交易发单");
		Identifier accountId = Identifier.of(dto.getAccountId());
		if(!accMgr.contains(accountId)) {
			throw new NoSuchGatewayException(dto.getAccountId());
		}
		Contract contract = marketCenter.getContract(Identifier.of(dto.getContractId())).contract();
		IAccount account = accMgr.get(accountId);
		Gateway gateway = account.getTradeGateway();
		if(gateway.getConnection() != ConnectionState.CONNECTED) {
			throw new NoConnectionException();
		}
		SubmitOrderReq.SubmitOrderReqBuilder builder = SubmitOrderReq.builder()
				.originOrderId(UUID.randomUUID().toString())
				.contingentCondition(ContingentConditionEnum.CC_Immediately)
				.contract(contract)
				.gatewayId(dto.getAccountId())
				.direction(dto.getDirection())
				.offsetFlag(dto.getAction())
				.volume(dto.getVolume())
				.currency(contract.currency())
				.price(dto.getPrice())	// 自动加上超价
				.volumeCondition(VolumeConditionEnum.VC_AV)
				.timeCondition(dto.isAnyPrice() ? TimeConditionEnum.TC_IOC : TimeConditionEnum.TC_GFD)
				.orderPriceType(dto.isAnyPrice() ? OrderPriceTypeEnum.OPT_AnyPrice : OrderPriceTypeEnum.OPT_LimitPrice)
				.contingentCondition(ContingentConditionEnum.CC_Immediately)
				.actionTimestamp(System.currentTimeMillis())
				.minVolume(1);
		
		if(contract.exchange() == ExchangeEnum.SHFE && FieldUtils.isClose(dto.getAction())) {
			Position p = null;
			switch(dto.getDirection()) {
			case D_Buy -> {
				p = account.getPosition(PositionDirectionEnum.PD_Short, contract).get();
			}
			case D_Sell -> {
				p = account.getPosition(PositionDirectionEnum.PD_Long, contract).get();
			}
			default -> throw new IllegalArgumentException("未知方向：" + dto.getDirection());
			}
			
			
			int ydAvailable = p.ydPosition() - p.ydFrozen();
			if(ydAvailable > dto.getVolume()) {
				account.submitOrder(builder
						.offsetFlag(OffsetFlagEnum.OF_Close)
						.volume(dto.getVolume())
						.build());
			} else {
				if(ydAvailable > 0) {
					account.submitOrder(builder
							.offsetFlag(OffsetFlagEnum.OF_Close)
							.volume(ydAvailable)
							.build());
				}
				if(dto.getVolume() - ydAvailable > 0) {					
					account.submitOrder(builder
							.offsetFlag(OffsetFlagEnum.OF_CloseToday)
							.volume(dto.getVolume() - ydAvailable)
							.build());
				}
			}
			
			return;
		}
		account.submitOrder(builder.build());
	}
	
	public void cancelOrder(OrderWithdrawDTO dto) {
		log.info("手工交易撤单");
		Identifier accountId = Identifier.of(dto.getAccountId());
		IAccount account = accMgr.get(accountId);
		Gateway gateway = account.getTradeGateway();
		if(gateway.getConnection() != ConnectionState.CONNECTED) {
			throw new NoConnectionException();
		}
		account.cancelOrder(dto.getOriginOrderId());
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
		return dealRepo.findByModuleName(accountId).stream()
				.map(ModuleDealRecordDO::convertTo)
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
