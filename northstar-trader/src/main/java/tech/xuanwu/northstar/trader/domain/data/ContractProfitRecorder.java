package tech.xuanwu.northstar.trader.domain.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import tech.xuanwu.northstar.persistance.ContractProfitRepo;
import tech.xuanwu.northstar.persistance.po.ContractProfit;
import tech.xuanwu.northstar.persistance.po.TradePair;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 合约盈亏记录器
 * 负责分发成交记录
 * @author kevinhuangwl
 *
 */
@Component
public class ContractProfitRecorder{

	@Autowired
	private ContractProfitRepo repo;
	
	public void record(TradeField trade) {
		if(trade.getOffsetFlag() == OffsetFlagEnum.OF_Unkonwn) {
			throw new IllegalStateException("未知开平仓方向 ");
		}
		String gatewayId = trade.getGatewayId();
		String unifiedSymbol = trade.getContract().getUnifiedSymbol();
		
		ContractProfit po = repo.findByGatewayIdAndUnifiedSymbol(gatewayId, unifiedSymbol);
		if(po == null) {
			po = new ContractProfit(gatewayId, unifiedSymbol);
		}
		if(trade.getOffsetFlag() == OffsetFlagEnum.OF_Open) {
			TradePair tradePair = new TradePair(trade);
			po.getTradePairRecords().add(tradePair);
		} else {
			for(TradePair tradePair : po.getTradePairRecords()) {
				if(tradePair.getOpenPositionVol() == 0) {
					continue;
				}
				trade = tradePair.closeWith(trade);
				if(trade == null) {
					break;
				}
			}
			if(trade != null) {
				throw new IllegalStateException("没有足够的开仓合约对应该平仓");
			}
		}
		po.updateProfit();
		repo.save(po);
	}

}
