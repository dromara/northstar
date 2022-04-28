package tech.quantit.northstar.domain.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.util.Assert;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.protobuf.InvalidProtocolBufferException;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.ClosingPolicy;
import tech.quantit.northstar.common.constant.ModuleState;
import tech.quantit.northstar.common.exception.NoSuchElementException;
import tech.quantit.northstar.common.model.ModuleAccountDescription;
import tech.quantit.northstar.common.model.ModuleDescription;
import tech.quantit.northstar.common.utils.FieldUtils;
import tech.quantit.northstar.strategy.api.IModuleAccountStore;
import tech.quantit.northstar.strategy.api.IModuleContext;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 模组账户状态存储器
 * @author KevinHuangwl
 *
 */
@Slf4j
public class ModuleAccountStore implements IModuleAccountStore {

	private IModuleContext ctx;
	
	/* gatewayId -> unifiedSymbol -> position */
	private Table<String, String, TradePosition> buyPositionTbl = HashBasedTable.create();
	private Table<String, String, TradePosition> sellPositionTbl = HashBasedTable.create();
	/* gatewayId -> commission */
	private Map<String, Double> commissionPerDealMap = new HashMap<>();
	/* gatewayId -> initBalance*/
	private Map<String, Double> initBalanceMap = new HashMap<>();
	/* gatewayId -> accDeal */
	private Map<String, Integer> accDealVolMap = new HashMap<>();
	/* gatewayId -> accCloseProfit */
	private Map<String, Double> accCloseProfitMap = new HashMap<>();
	
	public ModuleAccountStore(ModuleDescription moduleDescription) {
		ClosingPolicy closingPolicy = ctx.getClosingPolicy();
		for(ModuleAccountDescription mad : moduleDescription.getAccountDescriptions().values()) {
			initBalanceMap.put(mad.getAccountId(), mad.getInitBalance());
			commissionPerDealMap.put(mad.getAccountId(), mad.getCommissionPerDeal());
			accDealVolMap.put(mad.getAccountId(), mad.getAccDealVolume());
			accCloseProfitMap.put(mad.getAccountId(), mad.getAccCloseProfit());
			
			List<TradeField> allTrades = mad.getPositionDescription()
					.getUncloseTrades()
					.stream()
					.map(this::parseFrom)
					.filter(Objects::nonNull)
					.toList();
			
			for(TradeField trade : allTrades) {
				Assert.isTrue(trade.getDirection() != DirectionEnum.D_Unknown, "成交方向不正确");
				String gatewayId = trade.getGatewayId();
				String unifiedSymbol = trade.getContract().getUnifiedSymbol();
				Table<String, String, TradePosition> tbl = trade.getDirection() == DirectionEnum.D_Buy ? buyPositionTbl : sellPositionTbl;
				if(tbl.contains(gatewayId, unifiedSymbol)) {
					tbl.get(gatewayId, unifiedSymbol).onTrade(trade);
				} else {
					tbl.put(gatewayId, unifiedSymbol, new TradePosition(List.of(trade), closingPolicy));
				}
			}
		}
	}
	
	private TradeField parseFrom(byte[] data) {
		try {
			return TradeField.parseFrom(data);
		} catch (InvalidProtocolBufferException e) {
			log.warn("", e);
			return null;
		}
	}
	
	/* 更新 */
	@Override
	public void onOrder(OrderField order) {
		
	}

	@Override
	public void onTrade(TradeField trade) {
		// TODO Auto-generated method stub
		
	}
	
	/* 更新持仓盈亏 */
	@Override
	public void onTick(TickField tick) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setContext(IModuleContext context) {
		ctx = context;
	}

	@Override
	public double getInitBalance(String gatewayId) {
		if(!initBalanceMap.containsKey(gatewayId)) {
			throw new NoSuchElementException("找不到网关余额：" + gatewayId);
		}
		return initBalanceMap.get(gatewayId);
	}
	
	private double getCommission(String gatewayId) {
		if(!commissionPerDealMap.containsKey(gatewayId)) {
			throw new NoSuchElementException("找不到网关手续费：" + gatewayId);
		}
		return commissionPerDealMap.get(gatewayId);
	}

	@Override
	public double getPreBalance(String gatewayId) {
		return getInitBalance(gatewayId) + getAccCloseProfit(gatewayId) - getCommission(gatewayId) * getAccDealVolume(gatewayId);
	}

	@Override
	public List<TradeField> getUncloseTrades(String gatewayId) {
		Collection<TradePosition> buyOpenPositions = buyPositionTbl.row(gatewayId).values();
		Collection<TradePosition> sellOpenPositions = sellPositionTbl.row(gatewayId).values();
		List<TradeField> resultList = new ArrayList<>();
		buyOpenPositions.stream().forEach(tp -> resultList.addAll(tp.getUncloseTrades()));
		sellOpenPositions.stream().forEach(tp -> resultList.addAll(tp.getUncloseTrades()));
		return resultList;
	}

	@Override
	public List<TradeField> getUncloseTrade(String gatewayId, String unifiedSymbol, DirectionEnum dir) {
		Table<String, String, TradePosition> tbl = FieldUtils.isBuy(dir) ? buyPositionTbl : sellPositionTbl;
		if(tbl.contains(gatewayId, unifiedSymbol)) {
			return tbl.get(gatewayId, unifiedSymbol).getUncloseTrades();
		}
		return Collections.emptyList();
	}

	@Override
	public int getNetVolume(String gatewayId) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getNetProfit(String gatewayId) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getAccDealVolume(String gatewayId) {
		if(!accDealVolMap.containsKey(gatewayId)) {
			throw new NoSuchElementException("找不到网关交易数：" + gatewayId);
		}
		return accDealVolMap.get(gatewayId);
	}

	@Override
	public double getAccCloseProfit(String gatewayId) {
		if(!accCloseProfitMap.containsKey(gatewayId)) {
			throw new NoSuchElementException("找不到网关平仓盈亏：" + gatewayId);
		}
		return accCloseProfitMap.get(gatewayId);
	}

	@Override
	public double getAvailable(String gatewayId) {
		return getPreBalance(gatewayId);
	}

	@Override
	public void tradeDayPreset() {
		buyPositionTbl.values().stream().forEach(TradePosition::releaseOrders);
		sellPositionTbl.values().stream().forEach(TradePosition::releaseOrders);
	}

	@Override
	public ModuleState getModuleState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<PositionField> getPositions(String gatewayId) {
		// TODO Auto-generated method stub
		return null;
	}

}
