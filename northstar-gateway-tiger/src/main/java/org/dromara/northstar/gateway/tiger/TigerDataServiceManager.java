package org.dromara.northstar.gateway.tiger;

import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;

import java.time.LocalDate;
import java.util.List;

public class TigerDataServiceManager implements IDataSource {


    @Override
    public List<Bar> getMinutelyData(Contract contract, LocalDate startDate, LocalDate endDate) {
        return List.of();
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
