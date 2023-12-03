package org.dromara.northstar.gateway.playback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.TimeSlot;
import org.dromara.northstar.common.model.core.TradeTimeDefinition;
import org.dromara.northstar.common.utils.DateTimeUtils;
import org.springframework.stereotype.Component;

import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;

@Component
public class PlaybackContractDefProvider {
	
	private static final ExchangeEnum SSE = ExchangeEnum.SSE;
	private static final ExchangeEnum SZSE = ExchangeEnum.SZSE;
	private static final ExchangeEnum BSE = ExchangeEnum.BSE;
	private static final ExchangeEnum CFFEX = ExchangeEnum.CFFEX;
	private static final ExchangeEnum CZCE = ExchangeEnum.CZCE;
	private static final ExchangeEnum DCE = ExchangeEnum.DCE;
	private static final ExchangeEnum SHFE = ExchangeEnum.SHFE;
	private static final ExchangeEnum INE = ExchangeEnum.INE;
	private static final ExchangeEnum GFEX = ExchangeEnum.GFEX;
	
	private static final ProductClassEnum FUT = ProductClassEnum.FUTURES;
	private static final ProductClassEnum STK = ProductClassEnum.EQUITY;
	
	private static final String TT1 = "CN_FT_TT1";
	private static final String TT2 = "CN_FT_TT2";
	private static final String TT3 = "CN_FT_TT3";
	private static final String TT4 = "CN_FT_TT4";
	private static final String TT5 = "CN_FT_TT5";
	private static final String TT6 = "CN_FT_TT6";

	public List<ContractDefinition> get() {
		return List.of(
			build("沪A股", SSE, STK, ".+@SSE@.+", TT5, 3.0),
			build("深A股", SZSE, STK, ".+@SZSE@.+", TT5, 3.0),
			build("京A股", BSE, STK, ".+@BSE@.+", TT5, 3.0),
			build("IC", CFFEX, FUT, "IC[0-9]{3,4}@.+", TT5, 0.25),
			build("IH", CFFEX, FUT, "IH[0-9]{3,4}@.+", TT5, 0.25),
			build("IF", CFFEX, FUT, "IF[0-9]{3,4}@.+", TT5, 0.25),
			build("IM", CFFEX, FUT, "IM[0-9]{3,4}@.+", TT5, 0.25),
			build("三十债", CFFEX, FUT, "TL[0-9]{3,4}@.+", TT6, 400),
			build("十债", CFFEX, FUT, "T[0-9]{3,4}@.+", TT6, 400),
			build("五债", CFFEX, FUT, "TF[0-9]{3,4}@.+", TT6, 400),
			build("二债", CFFEX, FUT, "TS[0-9]{3,4}@.+", TT6, 400),
			build("甲醇", CZCE, FUT, "MA[0-9]{3,4}@.+", TT1, 2.0),
			build("动力煤", CZCE, FUT, "ZC[0-9]{3,4}@.+", TT1, 3000),
			build("菜粕", CZCE, FUT, "RM[0-9]{3,4}@.+", TT1, 300),
			build("玻璃", CZCE, FUT, "FG[0-9]{3,4}@.+", TT1, 1000),
			build("菜油", CZCE, FUT, "OI[0-9]{3,4}@.+", TT1, 500),
			build("白糖", CZCE, FUT, "SR[0-9]{3,4}@.+", TT1, 500),
			build("棉花", CZCE, FUT, "CF[0-9]{3,4}@.+", TT1, 1000),
			build("PTA", CZCE, FUT, "TA[0-9]{3,4}@.+", TT1, 600),
			build("棉纱", CZCE, FUT, "CY[0-9]{3,4}@.+", TT1, 400),
			build("短纤", CZCE, FUT, "PF[0-9]{3,4}@.+", TT1, 400),
			build("纯碱", CZCE, FUT, "SA[0-9]{3,4}@.+", TT1, 3.0),
			build("晚稻", CZCE, FUT, "LR[0-9]{3,4}@.+", TT4, 400),
			build("尿素", CZCE, FUT, "UR[0-9]{3,4}@.+", TT4, 3.0),
			build("苹果", CZCE, FUT, "AP[0-9]{3,4}@.+", TT4, 2500),
			build("早稻", CZCE, FUT, "RI[0-9]{3,4}@.+", TT4, 250),
			build("油菜籽", CZCE, FUT, "RS[0-9]{3,4}@.+", TT4, 1.5),
			build("粳稻", CZCE, FUT, "JR[0-9]{3,4}@.+", TT4, 400),
			build("强麦", CZCE, FUT, "WH[0-9]{3,4}@.+", TT4, 500),
			build("硅铁", CZCE, FUT, "SF[0-9]{3,4}@.+", TT4, 400),
			build("锰硅", CZCE, FUT, "SM[0-9]{3,4}@.+", TT4, 400),
			build("普麦", CZCE, FUT, "PM[0-9]{3,4}@.+", TT4, 500),
			build("红枣", CZCE, FUT, "CJ[0-9]{3,4}@.+", TT4, 400),
			build("花生", CZCE, FUT, "PK[0-9]{3,4}@.+", TT4, 400),
			build("聚丙烯", DCE, FUT, "pp[0-9]{3,4}@.+", TT1, 200),
			build("乙二醇", DCE, FUT, "eg[0-9]{3,4}@.+", TT1, 500),
			build("焦煤", DCE, FUT, "jm[0-9]{3,4}@.+", TT1, 3.5),
			build("豆一", DCE, FUT, "a[0-9]{3,4}@.+", TT1, 200),
			build("豆二", DCE, FUT, "b[0-9]{3,4}@.+", TT1, 100),
			build("玉米", DCE, FUT, "c[0-9]{3,4}@.+", TT1, 120),
			build("铁矿石", DCE, FUT, "i[0-9]{3,4}@.+", TT1, 2.0),
			build("焦炭", DCE, FUT, "j[0-9]{3,4}@.+", TT1, 2.0),
			build("塑料", DCE, FUT, "l[0-9]{3,4}@.+", TT1, 500),
			build("豆粕", DCE, FUT, "m[0-9]{3,4}@.+", TT1, 500),
			build("淀粉", DCE, FUT, "cs[0-9]{3,4}@.+", TT1, 200),
			build("棕榈油", DCE, FUT, "p[0-9]{3,4}@.+", TT1, 500),
			build("聚氯乙烯", DCE, FUT, "v[0-9]{3,4}@.+", TT1, 100),
			build("液化气", DCE, FUT, "pg[0-9]{3,4}@.+", TT1, 600),
			build("豆油", DCE, FUT, "y[0-9]{3,4}@.+", TT1, 500),
			build("苯乙烯", DCE, FUT, "eb[0-9]{3,4}@.+", TT1, 400),
			build("粳米", DCE, FUT, "rr[0-9]{3,4}@.+", TT1, 100),
			build("鸡蛋", DCE, FUT, "jd[0-9]{3,4}@.+", TT4, 3.0),
			build("纤板", DCE, FUT, "fb[0-9]{3,4}@.+", TT4, 1.5),
			build("胶板", DCE, FUT, "bb[0-9]{3,4}@.+", TT4, 1.5),
			build("生猪", DCE, FUT, "lh[0-9]{3,4}@.+", TT4, 2.1),
			build("工业硅", GFEX, FUT, "si[0-9]{3,4}@.+", TT4, 1.5),
			build("燃油", INE, FUT, "lu[0-9]{3,4}@.+", TT1, 0.1),
			build("20号胶", INE, FUT, "nr[0-9]{3,4}@.+", TT1, 0.2),
			build("国际油", INE, FUT, "bc[0-9]{3,4}@.+", TT2, 0.1),
			build("原油", INE, FUT, "sc[0-9]{3,4}@.+", TT3, 3000),
			build("螺纹钢", SHFE, FUT, "rb[0-9]{3,4}@.+", TT1, 2.0),
			build("橡胶", SHFE, FUT, "ru[0-9]{3,4}@.+", TT1, 400),
			build("燃料油", SHFE, FUT, "fu[0-9]{3,4}@.+", TT1, 2.0),
			build("沥青", SHFE, FUT, "bu[0-9]{3,4}@.+", TT1, 2.0),
			build("漂针浆", SHFE, FUT, "sp[0-9]{3,4}@.+", TT1, 0.5),
			build("热卷", SHFE, FUT, "hc[0-9]{3,4}@.+", TT1, 2.0),
			build("沪铝", SHFE, FUT, "al[0-9]{3,4}@.+", TT2, 500),
			build("沪锌", SHFE, FUT, "zn[0-9]{3,4}@.+", TT2, 500),
			build("沪镍", SHFE, FUT, "ni[0-9]{3,4}@.+", TT2, 3000),
			build("沪锡", SHFE, FUT, "sn[0-9]{3,4}@.+", TT2, 3000),
			build("沪铜", SHFE, FUT, "cu[0-9]{3,4}@.+", TT2, 2.0),
			build("沪铅", SHFE, FUT, "pb[0-9]{3,4}@.+", TT2, 0.5),
			build("不锈钢", SHFE, FUT, "ss[0-9]{3,4}@.+", TT2, 1.5),
			build("黄金", SHFE, FUT, "au[0-9]{3,4}@.+", TT3, 2000),
			build("白银", SHFE, FUT, "ag[0-9]{3,4}@.+", TT3, 1.0),
			build("线材", SHFE, FUT, "wr[0-9]{3,4}@.+", TT4, 0.4)
		);
	}
	
	private TimeSlot tsNight1 = TimeSlot.builder().start(DateTimeUtils.fromCacheTime(21, 0)).end(DateTimeUtils.fromCacheTime(23, 0)).build();
	private TimeSlot tsNight2 = TimeSlot.builder().start(DateTimeUtils.fromCacheTime(21, 0)).end(DateTimeUtils.fromCacheTime(1, 0)).build();
	private TimeSlot tsNight3 = TimeSlot.builder().start(DateTimeUtils.fromCacheTime(21, 0)).end(DateTimeUtils.fromCacheTime(2, 30)).build();
	
	private TimeSlot tsDay1 = TimeSlot.builder().start(DateTimeUtils.fromCacheTime(9, 0)).end(DateTimeUtils.fromCacheTime(10, 15)).build();
	private TimeSlot tsDay2 = TimeSlot.builder().start(DateTimeUtils.fromCacheTime(10, 30)).end(DateTimeUtils.fromCacheTime(11, 30)).build();
	private TimeSlot tsDay3 = TimeSlot.builder().start(DateTimeUtils.fromCacheTime(9, 30)).end(DateTimeUtils.fromCacheTime(11, 30)).build();
	
	private TimeSlot tsNoon1 = TimeSlot.builder().start(DateTimeUtils.fromCacheTime(13, 30)).end(DateTimeUtils.fromCacheTime(15, 00)).build();
	private TimeSlot tsNoon2 = TimeSlot.builder().start(DateTimeUtils.fromCacheTime(13, 30)).end(DateTimeUtils.fromCacheTime(15, 15)).build();
	
	private Map<String, TradeTimeDefinition> timeDefMap = new HashMap<>() {
		private static final long serialVersionUID = 1L;

		{
			put(TT1, TradeTimeDefinition.builder().timeSlots(List.of(tsNight1, tsDay1, tsDay2, tsNoon1)).build());
			put(TT2, TradeTimeDefinition.builder().timeSlots(List.of(tsNight2, tsDay1, tsDay2, tsNoon1)).build());
			put(TT3, TradeTimeDefinition.builder().timeSlots(List.of(tsNight3, tsDay1, tsDay2, tsNoon1)).build());
			put(TT4, TradeTimeDefinition.builder().timeSlots(List.of(tsDay1, tsDay2, tsNoon1)).build());
			put(TT5, TradeTimeDefinition.builder().timeSlots(List.of(tsDay3, tsNoon1)).build());
			put(TT6, TradeTimeDefinition.builder().timeSlots(List.of(tsDay1, tsDay2, tsNoon2)).build());
		}
	};

	private ContractDefinition build(String name, ExchangeEnum exchange, ProductClassEnum productClass, String pattern, String time, double commissionInBP){
		TradeTimeDefinition tradeTimeDef = timeDefMap.get(time);
		return ContractDefinition.builder().name(name).exchange(exchange).productClass(productClass)
				.symbolPattern(Pattern.compile(pattern)).tradeTimeDef(tradeTimeDef).commissionRate(commissionInBP / 10000D).build();
	}
	
	private ContractDefinition build(String name, ExchangeEnum exchange, ProductClassEnum productClass, String pattern, String time, int commissionInCent){
		TradeTimeDefinition tradeTimeDef = timeDefMap.get(time);
		return ContractDefinition.builder().name(name).exchange(exchange).productClass(productClass)
				.symbolPattern(Pattern.compile(pattern)).tradeTimeDef(tradeTimeDef).commissionFee(commissionInCent / 100D).build();
	}
}
