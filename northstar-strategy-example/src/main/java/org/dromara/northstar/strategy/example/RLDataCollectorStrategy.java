package org.dromara.northstar.strategy.example;

import java.io.FileWriter;

import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.Setting;
import org.dromara.northstar.strategy.AbstractStrategy;
import org.dromara.northstar.strategy.StrategicComponent;

import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.text.csv.CsvWriter;
import cn.hutool.core.util.CharsetUtil;
import xyz.redtorch.pb.CoreField.BarField;

@StrategicComponent(RLDataCollectorStrategy.NAME)
public class RLDataCollectorStrategy extends AbstractStrategy{
    protected static final String NAME = "示例-RL数据收集策略";
    
    private String barFilePath = "D:\\Software\\Desktop\\northstar\\python-app\\data\\bar.csv";
    
    private InitParams params;

    boolean haveHeader = false;
    
    CsvWriter writer = CsvUtil.getWriter(barFilePath, CharsetUtil.CHARSET_UTF_8, true);
    
    public RLDataCollectorStrategy() {
        if (! haveHeader) {
            String header[] = {"Unified Symbol", "Action Time Stamp", "Action Day", "Action Time", "Open Price", "High Price", "Low Price", "Closed Price"};
            writer.write(header);
            haveHeader = true;
        }
    }

    @Override
    public void onMergedBar(BarField bar) {
        log.debug("写入{} {}数据", bar.getActionDay(), bar.getActionTime());
        String data[] = { 
                            bar.getUnifiedSymbol(), 
                            String.valueOf(bar.getActionTimestamp()), 
                            bar.getActionDay(),
                            bar.getActionTime(),
                            String.valueOf(bar.getOpenPrice()), 
                            String.valueOf(bar.getHighPrice()), 
                            String.valueOf(bar.getLowPrice()), 
                            String.valueOf(bar.getClosePrice())
                        };
        writer.write(data);
        if(!canProceed(bar)) {
			return;
		}
		if(barHandlerMap.containsKey(bar.getUnifiedSymbol())) {
			barHandlerMap.get(bar.getUnifiedSymbol()).onMergedBar(bar);
		}
    }

    @Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
		this.params = (InitParams) params;
	}
	public static class InitParams extends DynamicParams {
		@Setting(label="指标合约", order=0)
		private String indicatorSymbol;		
	}
}