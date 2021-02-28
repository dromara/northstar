package tech.xuanwu.northstar.trader.domain.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.data.Offset;
import org.junit.Test;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;

public class WinnerSMSResolverTest {
	
	@Test
	public void testResolve() {
		ContractField c1 = ContractField.newBuilder().setSymbol("rb2105").build();
		ContractField c2 = ContractField.newBuilder().setSymbol("J2105").build();
		
		Map<String, ContractField> contractMap = mock(Map.class);
		when(contractMap.get("RB2105")).thenReturn(c1);
		when(contractMap.get("J2105")).thenReturn(c2);
		
		Offset<Double> precise = Offset.offset(0.0001D);
		
		WinnerSMSResolver r = new WinnerSMSResolver(contractMap);
		SubmitOrderReqField[] req1 = r
				.resolve("【**期货】赢家10号：RB2105在4545.0的价格平空单，在4545.0的价格开多单，止损价：4505.0，目前持有多单1手（+1），仅供参考。");
		assertThat(StringUtils.equalsIgnoreCase(req1[0].getContract().getSymbol(), "RB2105")).isTrue();
		assertThat(req1[0].getPrice()).isCloseTo(4545, precise);
		assertThat(req1[0].getDirection()).isEqualTo(DirectionEnum.D_Buy);
		assertThat(req1[0].getOffsetFlag()).isEqualTo(OffsetFlagEnum.OF_ForceClose);
		assertThat(StringUtils.equalsIgnoreCase(req1[1].getContract().getSymbol(), "RB2105")).isTrue();
		assertThat(req1[1].getPrice()).isCloseTo(4545, precise);
		assertThat(req1[1].getStopPrice()).isCloseTo(4505, precise);
		assertThat(req1[1].getDirection()).isEqualTo(DirectionEnum.D_Buy);
		assertThat(req1[1].getOffsetFlag()).isEqualTo(OffsetFlagEnum.OF_Open);

		
		SubmitOrderReqField[] req2 = r
				.resolve("【**期货】赢家12号：J2105在2543.0的价格平多单，在2543.0的价格开空单，止损价：2559.5，目前持有空单1手（-1），仅供参考。");
		assertThat(StringUtils.equalsIgnoreCase(req2[0].getContract().getSymbol(), "J2105")).isTrue();
		assertThat(req2[0].getPrice()).isCloseTo(2543, precise);
		assertThat(req2[0].getDirection()).isEqualTo(DirectionEnum.D_Sell);
		assertThat(req2[0].getOffsetFlag()).isEqualTo(OffsetFlagEnum.OF_ForceClose);
		assertThat(StringUtils.equalsIgnoreCase(req2[1].getContract().getSymbol(), "J2105")).isTrue();
		assertThat(req2[1].getPrice()).isCloseTo(2543, precise);
		assertThat(req2[1].getStopPrice()).isCloseTo(2559.5, precise);
		assertThat(req2[1].getDirection()).isEqualTo(DirectionEnum.D_Sell);
		assertThat(req2[1].getOffsetFlag()).isEqualTo(OffsetFlagEnum.OF_Open);
		
		SubmitOrderReqField[] req3 = r
				.resolve("【**期货】赢家10号：RB2105在4612.0的价格平多单，目前无持仓（0），仅供参考。");
		assertThat(req3.length).isEqualTo(1);
		assertThat(StringUtils.equalsIgnoreCase(req3[0].getContract().getSymbol(), "RB2105")).isTrue();
		assertThat(req3[0].getPrice()).isCloseTo(4612, precise);
		assertThat(req3[0].getDirection()).isEqualTo(DirectionEnum.D_Sell);
		assertThat(req3[0].getOffsetFlag()).isEqualTo(OffsetFlagEnum.OF_ForceClose);
		
		SubmitOrderReqField[] req4 = r
				.resolve("【**期货】 赢家12号：J2105在2602.5的价格开空单，止损价：2668.0，目前持有空单1手（-1），仅供参考。");
		assertThat(req4.length).isEqualTo(1);
		assertThat(req4[0].getPrice()).isCloseTo(2602.5, precise);
		assertThat(req4[0].getStopPrice()).isCloseTo(2668, precise);
		assertThat(req4[0].getDirection()).isEqualTo(DirectionEnum.D_Sell);
		assertThat(req4[0].getOffsetFlag()).isEqualTo(OffsetFlagEnum.OF_Open);
	}

}
