package org.dromara.northstar.gateway.tiger;

import com.tigerbrokers.stock.openapi.client.https.domain.contract.item.ContractItem;
import com.tigerbrokers.stock.openapi.client.https.domain.contract.item.TickSizeItem;
import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.gateway.Instrument;
import xyz.redtorch.pb.CoreEnum.CurrencyEnum;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class TigerContract implements Instrument {

    private ContractItem item;

    private ContractDefinition contractDef;

    private IDataSource dataSrc;

    public TigerContract(ContractItem item, IDataSource dataSrc) {
        this.item = item;
        this.dataSrc = dataSrc;
    }

    @Override
    public String name() {
        return item.getSymbol() + "-" + item.getName();
    }

    @Override
    public Identifier identifier() {
        return Identifier.of(String.format("%s-%s@%s@%s@%s", item.getSymbol(), item.getName(), exchange(), productClass(), channelType()));
    }

    @Override
    public ProductClassEnum productClass() {
        return switch (item.getSecType()) {
            case "STK" -> ProductClassEnum.EQUITY;
            case "OPT" -> ProductClassEnum.OPTION;
            case "FUT" -> ProductClassEnum.FUTURES;
            case "WAR" -> ProductClassEnum.WARRANTS;
            case "IOPT" -> ProductClassEnum.SPOTOPTION;
            default -> throw new IllegalArgumentException("Unexpected value: " + item.getSecType());
        };
    }

    @Override
    public ExchangeEnum exchange() {
        //交易所(exchange)，STK类型的合约一般不会用到交易所字段，订单会自动路由，期货合约都用到交易所字段。
        if (item.getExchange() == null) {
            return ExchangeEnum.SMART;
        } else {
            return switch (item.getExchange()) {
                case "SMART", "VALUE" -> ExchangeEnum.SMART;
                case "SEHKSZSE" -> ExchangeEnum.SZSE;
                case "SEHKNTL" -> ExchangeEnum.SSE;
                case "SEHK" -> ExchangeEnum.SEHK;
                default -> throw new IllegalArgumentException("Unexpected value: " + item.getExchange());
            };
        }
    }

    @Override
    public ChannelType channelType() {
        return ChannelType.TIGER;
    }

    @Override
    public Contract contract() {
        // 检查是否有有效的 tickSizes 数据
        Double minTick = item.getMinTick(); // 默认最小报价单位
        List<TickSizeItem> tickSizes = item.getTickSizes();

        if ("STK".equals(item.getSecType())) {
            // 假设我们使用第一个 tickSize 的 tickSize 作为最小报价单位
            minTick = tickSizes.getFirst().getTickSize();
        } else if (minTick == null && tickSizes != null) {
            minTick = tickSizes.getFirst().getTickSize();
        }
        String minTickStr = String.valueOf(minTick);
        int pricePrecision = minTickStr.contains(".") ? minTickStr.length() - minTickStr.indexOf('.') - 1 : 0;

        return Contract.builder()
                .gatewayId(ChannelType.TIGER.toString())
                .symbol(item.getSymbol())
                .unifiedSymbol(String.format("%s-%s@%s@%s", item.getSymbol(), item.getName(), exchange(), productClass()))
                .name(item.getName())
                .fullName(item.getName())
                .currency(CurrencyEnum.valueOf(item.getCurrency()))
                .exchange(exchange())
                .productClass(productClass())
                .contractId(identifier().value())
                .multiplier(Optional.ofNullable(item.getMultiplier()).orElse(1D))
                .priceTick(minTick)
                .pricePrecision(pricePrecision)
                .longMarginRatio(Optional.ofNullable(item.getLongInitialMargin()).orElse(0D))
                .shortMarginRatio(Optional.ofNullable(item.getShortInitialMargin()).orElse(0D))
                .lastTradeDate(LocalDate.parse(Optional.ofNullable(item.getContractMonth()).orElse(String.valueOf(LocalDate.now()))))
                //.strikePrice(Optional.ofNullable(item.getStrike()).orElse(0D))
                .thirdPartyId(String.format("%s@TIGER", item.getSymbol()))
                .channelType(ChannelType.TIGER)
                .tradable(true)
                .contractDefinition(contractDef)
                .build();
    }

    @Override
    public IDataSource dataSource() {
        return dataSrc;
    }

    @Override
    public void setContractDefinition(ContractDefinition contractDef) {
        this.contractDef = contractDef;
    }

}
