package org.dromara.northstar.gateway.tiger;

import com.tigerbrokers.stock.openapi.client.config.ClientConfig;
import com.tigerbrokers.stock.openapi.client.https.domain.quote.item.KlinePoint;
import com.tigerbrokers.stock.openapi.client.struct.enums.KType;
import com.tigerbrokers.stock.openapi.client.struct.enums.Language;
import lombok.extern.slf4j.Slf4j;
import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.ObjectManager;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.gateway.Gateway;
import org.dromara.northstar.gateway.tiger.util.PageTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;

@Slf4j
public class TigerDataServiceManager implements IDataSource {
    @Autowired
    private TigerGatewaySettings settings;
    @Autowired
    private ObjectManager<Gateway> gatewayManager;

    private TigerHttpClient getTigerHttpClient() {
        Gateway gateway = gatewayManager.get(Identifier.of(ChannelType.TIGER.toString()));
        settings = (TigerGatewaySettings) gateway.gatewayDescription().getSettings();

        ClientConfig clientConfig = ClientConfig.DEFAULT_CONFIG;
        clientConfig.tigerId = settings.getTigerId();
        clientConfig.defaultAccount = settings.getAccountId();
        clientConfig.privateKey = settings.getPrivateKey();
        clientConfig.license = settings.getLicense();
        clientConfig.secretKey = settings.getSecretKey();
        clientConfig.language = Language.zh_CN;
        return TigerHttpClient.getInstance().clientConfig(clientConfig);
    }

    @Override
    public List<Bar> getMinutelyData(Contract contract, LocalDate startDate, LocalDate endDate) {
        TigerHttpClient client = getTigerHttpClient();
        LinkedList<Bar> barFieldList = new LinkedList<>();
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            // 将 LocalDate 转换为 LocalDateTime 并格式化为字符串
            String beginTime = startDate.atStartOfDay().format(formatter);
            String endTime = endDate.atStartOfDay().format(formatter);
            List<KlinePoint> klinePoints = PageTokenUtil.getKlineByPage(
                    client,
                    contract.symbol(),
                    KType.min1,
                    beginTime,
                    endTime);

            for (KlinePoint klinePoint : klinePoints) {
                Instant e = Instant.ofEpochMilli(klinePoint.getTime());
                LocalTime actionTime = e.atZone(ZoneId.systemDefault()).toLocalTime();
                LocalDate tradingDay = e.atZone(ZoneId.systemDefault()).toLocalDate();

                barFieldList.addFirst(Bar.builder()
                        .contract(contract)
                        .gatewayId(contract.gatewayId())
                        .tradingDay(tradingDay)
                        .actionDay(tradingDay)
                        .actionTime(actionTime)
                        .actionTimestamp(klinePoint.getTime())
                        .openPrice(klinePoint.getOpen())
                        .highPrice(klinePoint.getHigh())
                        .lowPrice(klinePoint.getLow())
                        .closePrice(klinePoint.getClose())
                        .volume(klinePoint.getVolume())
                        .volumeDelta(klinePoint.getVolume())
                        .turnover(klinePoint.getAmount())
                        .turnoverDelta(klinePoint.getAmount())
                        .channelType(ChannelType.TIGER)
                        .build());
            }
        } catch (Exception e) {
            log.error("", e);
        }
        return barFieldList;
    }

    @Override
    public List<Bar> getQuarterlyData(Contract contract, LocalDate startDate, LocalDate endDate) {
        return List.of();
    }

    @Override
    public List<Bar> getHourlyData(Contract contract, LocalDate startDate, LocalDate endDate) {
        return List.of();
    }

    @Override
    public List<Bar> getDailyData(Contract contract, LocalDate startDate, LocalDate endDate) {
        return List.of();
    }

    @Override
    public List<LocalDate> getHolidays(ChannelType channelType, LocalDate startDate, LocalDate endDate) {
        return List.of();
    }

    @Override
    public List<Contract> getAllContracts() {
        return List.of();
    }
}
