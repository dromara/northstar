package tech.quantit.northstar.gateway.binance;

import lombok.Getter;
import lombok.Setter;
import tech.quantit.northstar.common.model.DynamicParams;
import tech.quantit.northstar.common.model.GatewaySettings;

@Getter
@Setter
public class BinanceGatewaySettings extends DynamicParams implements GatewaySettings{

	private String scretKey;
}
