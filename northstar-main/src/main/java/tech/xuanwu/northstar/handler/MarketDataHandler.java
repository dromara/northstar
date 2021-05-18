package tech.xuanwu.northstar.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.common.utils.BarGenerator;
import tech.xuanwu.northstar.engine.event.FastEventEngine;
import tech.xuanwu.northstar.persistence.MarketDataRepository;
import tech.xuanwu.northstar.persistence.po.MinBarDataPO;
import tech.xuanwu.northstar.persistence.po.TickDataPO;
import tech.xuanwu.northstar.utils.ProtoBeanUtils;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 处理行情相关操作
 * 
 * @author KevinHuangwl
 *
 */
@Slf4j
public class MarketDataHandler extends AbstractEventHandler implements InternalEventHandler {

	/**
	 * gateway -> unifiedSymbol -> generator
	 */
	private Table<String, String, BarGenerator> generatorTbl = HashBasedTable.create();

	private FastEventEngine feEngine;
	
	private MarketDataRepository mdRepo;

	public MarketDataHandler(FastEventEngine feEngine, MarketDataRepository mdRepo) {
		this.feEngine = feEngine;
		this.mdRepo = mdRepo;
	}

	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return NorthstarEventType.TICK == eventType || NorthstarEventType.IDX_TICK == eventType;
	}

	@Override
	protected void doHandle(NorthstarEvent e) {
		TickField tick = (TickField) e.getData();
		String unifiedSymbol = tick.getUnifiedSymbol();
		String gatewayId = tick.getGatewayId();
		if (!generatorTbl.contains(gatewayId, unifiedSymbol)) {
			generatorTbl.put(gatewayId, unifiedSymbol, new BarGenerator(unifiedSymbol, (bar, ticks) -> {
				feEngine.emitEvent(NorthstarEventType.BAR, bar);
				try {					
					MinBarDataPO barPO = ProtoBeanUtils.toPojoBean(MinBarDataPO.class, bar);
					List<TickDataPO> minTicks = new ArrayList<>(ticks.size());
					for(TickField t : ticks) {
						TickDataPO tickPO = ProtoBeanUtils.toPojoBean(TickDataPO.class, t);
						minTicks.add(tickPO);
					}
					barPO.setMinuteTickData(minTicks);
					mdRepo.insert(barPO);
				}catch(IOException ex) {
					throw new IllegalStateException(ex);
				}
			}));
		}
		
		generatorTbl.get(gatewayId, unifiedSymbol).updateTick(tick);
	}

}
