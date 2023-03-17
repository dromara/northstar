package tech.quantit.northstar.data.ds;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import tech.quantit.northstar.common.constant.ChannelType;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.data.IMarketDataRepository;
import tech.quantit.northstar.data.ds.factory.DataManagerFactory;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreField.BarField;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Slf4j
public class W3MarketDataRepoDataServiceImpl implements IMarketDataRepository {

    private static final String EMPTY_IMPLEMENTATION_HINT = "采用历史行情数据服务适配器时，不实现该方法";

    @Autowired
    private DataManagerFactory dmf;

    public W3MarketDataRepoDataServiceImpl(DataManagerFactory dmf) {
        this.dmf = dmf;
    }

    @Override
    public void insert(BarField bar) {
        log.trace(EMPTY_IMPLEMENTATION_HINT);
    }

    @Override
    public List<BarField> loadBars(String unifiedSymbol, LocalDate startDate, LocalDate endDate) {
        log.debug("从数据服务加载历史行情分钟数据：{}，{} -> {}", unifiedSymbol, startDate.format(DateTimeConstant.D_FORMAT_INT_FORMATTER), endDate.format(DateTimeConstant.D_FORMAT_INT_FORMATTER));
        try {
            return dmf.getDmBySymbol(unifiedSymbol).getMinutelyData(unifiedSymbol, startDate, endDate);
        } catch (Exception e) {
            log.warn("第三方数据服务暂时不可用：{}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<BarField> loadDailyBars(String unifiedSymbol, LocalDate startDate, LocalDate endDate) {
        log.debug("从数据服务加载历史行情日数据：{}，{} -> {}", unifiedSymbol, startDate.format(DateTimeConstant.D_FORMAT_INT_FORMATTER), endDate.format(DateTimeConstant.D_FORMAT_INT_FORMATTER));
        try {
            return dmf.getDmBySymbol(unifiedSymbol).getDailyData(unifiedSymbol, startDate, endDate);
        } catch (Exception e) {
            log.warn("第三方数据服务暂时不可用：{}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<LocalDate> findHodidayInLaw(String gatewayType, int year) {
        List<LocalDate> resultList;
        try {
            resultList = dmf.getDm(ChannelType.valueOf(gatewayType)).getHolidays(ExchangeEnum.SHFE, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
        } catch (Exception e) {
            log.warn("第三方数据服务暂时不可用：{}", e.getMessage(), e);
            return Collections.emptyList();
        }
        return resultList.stream()
                .filter(date -> date.getDayOfWeek().getValue() < 6)
                .toList();
    }

}
