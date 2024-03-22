package org.dromara.northstar.gateway.playback;

import java.util.List;
import java.util.regex.Pattern;

import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.TimeSlot;
import org.dromara.northstar.common.model.core.TradeTimeDefinition;
import org.dromara.northstar.common.utils.DateTimeUtils;
import org.dromara.northstar.gateway.mktdata.NorthstarDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;

@Component
public class PlaybackContractDefProvider {
	
	@Autowired
	private NorthstarDataSource dataSource;
	
	private static final ExchangeEnum SSE = ExchangeEnum.SSE;
	private static final ExchangeEnum SZSE = ExchangeEnum.SZSE;
	private static final ExchangeEnum BSE = ExchangeEnum.BSE;
	
	private static final ProductClassEnum FUT = ProductClassEnum.FUTURES;
	private static final ProductClassEnum STK = ProductClassEnum.EQUITY;
	private static final ProductClassEnum OPT = ProductClassEnum.OPTION;
	private static final ProductClassEnum SPT = ProductClassEnum.SPOTOPTION;
	
	private static final ExchangeEnum CFFEX = ExchangeEnum.CFFEX;
	private static final ExchangeEnum CZCE = ExchangeEnum.CZCE;
	private static final ExchangeEnum DCE = ExchangeEnum.DCE;
	private static final ExchangeEnum SHFE = ExchangeEnum.SHFE;
	private static final ExchangeEnum INE = ExchangeEnum.INE;
	private static final ExchangeEnum GFEX = ExchangeEnum.GFEX;
	
	public List<ContractDefinition> get() {
		return List.of(
			/* 夜盘1品种 */
			build("甲醇", CZCE, FUT, "MA[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 2.0),
			build("甲醇", CZCE, OPT, "MA[0-9]{3,4}[^@].+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 50),
			build("动力煤", CZCE, FUT, "ZC[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 3000),
			build("动力煤", CZCE, OPT, "ZC[0-9]{3,4}[^@].+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 200),
			build("菜粕", CZCE, FUT, "RM[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 300),
			build("菜粕", CZCE, OPT, "RM[0-9]{3,4}[^@].+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 80),
			build("白糖", CZCE, FUT, "SR[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 500),
			build("白糖", CZCE, OPT, "SR[0-9]{3,4}[^@].+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 200),
			build("棉花", CZCE, FUT, "CF[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 1000),
			build("棉花", CZCE, OPT, "CF[0-9]{3,4}[^@].+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 200),
			build("玻璃", CZCE, FUT, "FG[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 1000),
			build("菜油", CZCE, FUT, "OI[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 500),
			build("菜油", CZCE, OPT, "OI[0-9]{3,4}[^@].+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 200),
			build("PTA", CZCE, FUT, "TA[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 600),
			build("PTA", CZCE, OPT, "TA[0-9]{3,4}[^@].+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 50),
			build("棉纱", CZCE, FUT, "CY[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 400),
			build("短纤", CZCE, FUT, "PF[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 400),
			build("短纤", CZCE, OPT, "PF[0-9]{3,4}[^@].+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 100),
			build("烧碱", CZCE, FUT, "SH[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 1.0),
			build("烧碱", CZCE, OPT, "SH[0-9]{3,4}[^@].+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 300),
			build("纯碱", CZCE, FUT, "SA[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 3.0),
			build("纯碱", CZCE, OPT, "SA[0-9]{3,4}[^@].+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 100),
			build("聚丙烯", DCE, FUT, "pp[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 200),
			build("聚丙烯", DCE, OPT, "pp[0-9]{3,4}[^@].+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 50),
			build("豆油", DCE, FUT, "y[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 500),
			build("豆油", DCE, OPT, "y[0-9]{3,4}[^@].+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 50),
			build("乙二醇", DCE, FUT, "eg[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 500),
			build("乙二醇", DCE, OPT, "eg[0-9]{3,4}[^@].+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 400),
			build("焦煤", DCE, FUT, "jm[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 3.5),
			build("豆一", DCE, FUT, "a[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 200),
			build("豆一", DCE, OPT, "a[0-9]{3,4}[^@].+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 50),
			build("豆二", DCE, FUT, "b[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 100),
			build("豆二", DCE, OPT, "b[0-9]{3,4}[^@].+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 20),
			build("玉米", DCE, FUT, "c[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 120),
			build("玉米", DCE, OPT, "c[0-9]{3,4}[^@].+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 60),
			build("铁矿石", DCE, FUT, "i[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 2.0),
			build("铁矿石", DCE, OPT, "i[0-9]{3,4}[^@].+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 200),
			build("焦炭", DCE, FUT, "j[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 2.0),
			build("塑料", DCE, FUT, "l[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 500),
			build("塑料", DCE, OPT, "l[0-9]{3,4}[^@].+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 50),
			build("豆粕", DCE, FUT, "m[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 500),
			build("豆粕", DCE, OPT, "m[0-9]{3,4}[^@].+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 100),
			build("淀粉", DCE, FUT, "cs[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 200),
			build("棕榈油", DCE, FUT, "p[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 500),
			build("棕榈油", DCE, OPT, "p[0-9]{3,4}[^@].+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 50),
			build("聚氯乙烯", DCE, FUT, "v[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 100),
			build("聚氯乙烯", DCE, OPT, "v[0-9]{3,4}[^@].+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 50),
			build("液化气", DCE, FUT, "pg[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 600),
			build("液化气", DCE, OPT, "pg[0-9]{3,4}[^@].+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 100),
			build("苯乙烯", DCE, FUT, "eb[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 400),
			build("苯乙烯", DCE, OPT, "eb[0-9]{3,4}[^@].+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 400),
			build("粳米", DCE, FUT, "rr[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 100),
			build("燃油", INE, FUT, "lu[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 0.1),
			build("20号胶", INE, FUT, "nr[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 0.2),
			build("BR橡胶", SHFE, FUT, "br[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 1.0),
			build("BR橡胶", SHFE, OPT, "br[0-9]{3,4}[^@].+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 100),
			build("螺纹钢", SHFE, FUT, "rb[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 2.0),
			build("螺纹钢", SHFE, OPT, "rb[0-9]{3,4}[^@].+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 1.5),
			build("橡胶", SHFE, FUT, "ru[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 400),
			build("橡胶", SHFE, OPT, "ru[0-9]{3,4}[^@].+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 400),
			build("燃料油", SHFE, FUT, "fu[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 2.0),
			build("沥青", SHFE, FUT, "bu[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 2.0),
			build("纸浆", SHFE, FUT, "sp[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 1.0),
			build("热卷", SHFE, FUT, "hc[0-9]{3,4}@.+", List.of(t2100_2300, t0900_1015, t1030_1130, t1330_1500), 2.0),
			/* 夜盘2品种 */
			build("国际油", INE, FUT, "bc[0-9]{3,4}@.+", List.of(t2100_0100, t0900_1015, t1030_1130, t1330_1500), 0.1),
			build("沪铝", SHFE, FUT, "al[0-9]{3,4}@.+", List.of(t2100_0100, t0900_1015, t1030_1130, t1330_1500), 500),
			build("沪铝", SHFE, OPT, "al[0-9]{3,4}[^@].+", List.of(t2100_0100, t0900_1015, t1030_1130, t1330_1500), 200),
			build("沪锌", SHFE, FUT, "zn[0-9]{3,4}@.+", List.of(t2100_0100, t0900_1015, t1030_1130, t1330_1500), 500),
			build("沪锌", SHFE, OPT, "zn[0-9]{3,4}[^@].+", List.of(t2100_0100, t0900_1015, t1030_1130, t1330_1500), 200),
			build("沪铜", SHFE, FUT, "cu[0-9]{3,4}@.+", List.of(t2100_0100, t0900_1015, t1030_1130, t1330_1500), 2.0),
			build("沪铜", SHFE, OPT, "cu[0-9]{3,4}[^@].+", List.of(t2100_0100, t0900_1015, t1030_1130, t1330_1500), 500),
			build("沪镍", SHFE, FUT, "ni[0-9]{3,4}@.+", List.of(t2100_0100, t0900_1015, t1030_1130, t1330_1500), 3000),
			build("沪锡", SHFE, FUT, "sn[0-9]{3,4}@.+", List.of(t2100_0100, t0900_1015, t1030_1130, t1330_1500), 3000),
			build("沪铅", SHFE, FUT, "pb[0-9]{3,4}@.+", List.of(t2100_0100, t0900_1015, t1030_1130, t1330_1500), 0.5),
			build("不锈钢", SHFE, FUT, "ss[0-9]{3,4}@.+", List.of(t2100_0100, t0900_1015, t1030_1130, t1330_1500), 1.5),
			/* 夜盘3品种 */
			build("原油", INE, FUT, "sc[0-9]{3,4}@.+", List.of(t2100_0230, t0900_1015, t1030_1130, t1330_1500), 3000),
			build("原油", INE, OPT, "sc[0-9]{3,4}[^@].+", List.of(t2100_0230, t0900_1015, t1030_1130, t1330_1500), 400),
			build("黄金", SHFE, FUT, "au[0-9]{3,4}@.+", List.of(t2100_0230, t0900_1015, t1030_1130, t1330_1500), 2000),
			build("黄金", SHFE, OPT, "au[0-9]{3,4}[^@].+", List.of(t2100_0230, t0900_1015, t1030_1130, t1330_1500), 200),
			build("白银", SHFE, FUT, "ag[0-9]{3,4}@.+", List.of(t2100_0230, t0900_1015, t1030_1130, t1330_1500), 1.0),
			build("白银", SHFE, OPT, "ag[0-9]{3,4}[^@].+", List.of(t2100_0230, t0900_1015, t1030_1130, t1330_1500), 100),
			/* 日盘1品种 */
			build("晚稻", CZCE, FUT, "LR[0-9]{3,4}@.+", List.of(t0900_1015, t1030_1130, t1330_1500), 400),
			build("尿素", CZCE, FUT, "UR[0-9]{3,4}@.+", List.of(t0900_1015, t1030_1130, t1330_1500), 3.0),
			build("尿素", CZCE, OPT, "UR[0-9]{3,4}[^@].+", List.of(t0900_1015, t1030_1130, t1330_1500), 150),
			build("苹果", CZCE, FUT, "AP[0-9]{3,4}@.+", List.of(t0900_1015, t1030_1130, t1330_1500), 2500),
			build("苹果", CZCE, OPT, "AP[0-9]{3,4}[^@].+", List.of(t0900_1015, t1030_1130, t1330_1500), 150),
			build("早稻", CZCE, FUT, "RI[0-9]{3,4}@.+", List.of(t0900_1015, t1030_1130, t1330_1500), 250),
			build("油菜籽", CZCE, FUT, "RS[0-9]{3,4}@.+", List.of(t0900_1015, t1030_1130, t1330_1500), 1.5),
			build("粳稻", CZCE, FUT, "JR[0-9]{3,4}@.+", List.of(t0900_1015, t1030_1130, t1330_1500), 400),
			build("强麦", CZCE, FUT, "WH[0-9]{3,4}@.+", List.of(t0900_1015, t1030_1130, t1330_1500), 500),
			build("硅铁", CZCE, FUT, "SF[0-9]{3,4}@.+", List.of(t0900_1015, t1030_1130, t1330_1500), 400),
			build("硅铁", CZCE, OPT, "SF[0-9]{3,4}[^@].+", List.of(t0900_1015, t1030_1130, t1330_1500), 100),
			build("锰硅", CZCE, FUT, "SM[0-9]{3,4}@.+", List.of(t0900_1015, t1030_1130, t1330_1500), 400),
			build("锰硅", CZCE, OPT, "SM[0-9]{3,4}[^@].+", List.of(t0900_1015, t1030_1130, t1330_1500), 100),
			build("普麦", CZCE, FUT, "PM[0-9]{3,4}@.+", List.of(t0900_1015, t1030_1130, t1330_1500), 500),
			build("红枣", CZCE, FUT, "CJ[0-9]{3,4}@.+", List.of(t0900_1015, t1030_1130, t1330_1500), 400),
			build("花生", CZCE, FUT, "PK[0-9]{3,4}@.+", List.of(t0900_1015, t1030_1130, t1330_1500), 400),
			build("花生", CZCE, OPT, "PK[0-9]{3,4}[^@].+", List.of(t0900_1015, t1030_1130, t1330_1500), 80),
			build("对二甲苯", CZCE, FUT, "PX[0-9]{3,4}@.+", List.of(t0900_1015, t1030_1130, t1330_1500), 1.0),
			build("对二甲苯", CZCE, OPT, "PX[0-9]{3,4}[^@].+", List.of(t0900_1015, t1030_1130, t1330_1500), 100),
			build("鸡蛋", DCE, FUT, "jd[0-9]{3,4}@.+", List.of(t0900_1015, t1030_1130, t1330_1500), 3.0),
			build("纤板", DCE, FUT, "fb[0-9]{3,4}@.+", List.of(t0900_1015, t1030_1130, t1330_1500), 1.5),
			build("胶板", DCE, FUT, "bb[0-9]{3,4}@.+", List.of(t0900_1015, t1030_1130, t1330_1500), 1.5),
			build("生猪", DCE, FUT, "lh[0-9]{3,4}@.+", List.of(t0900_1015, t1030_1130, t1330_1500), 2.1),
			build("氧化铝", SHFE, FUT, "ao[0-9]{3,4}@.+", List.of(t0900_1015, t1030_1130, t1330_1500), 1.0),
			build("线材", SHFE, FUT, "wr[0-9]{3,4}@.+", List.of(t0900_1015, t1030_1130, t1330_1500), 0.4),
			build("集运欧线", SHFE, FUT, "ec[0-9]{3,4}@.+", List.of(t0900_1015, t1030_1130, t1330_1500), 2.0),
			/* 日盘2品种 */
			build("碳酸锂", GFEX, FUT, "lc[0-9]{3,4}@.+", List.of(t0900_1130, t1330_1500), 0.8),
			build("工业硅", GFEX, FUT, "si[0-9]{3,4}@.+", List.of(t0900_1130, t1330_1500), 1.5),
			build("工业硅", GFEX, OPT, "si[0-9]{3,4}[^@].+", List.of(t0900_1130, t1330_1500), 200),
			/* 股指品种 */
			build("沪A股", SSE, STK, ".+@SSE@.+", List.of(t0930_1130, t1300_1500), 3.0),
			build("深A股", SZSE, STK, ".+@SZSE@.+", List.of(t0930_1130, t1300_1500), 3.0),
			build("京A股", BSE, STK, ".+@BSE@.+", List.of(t0930_1130, t1300_1500), 3.0),
			build("IC", CFFEX, FUT, "IC[0-9]{3,4}@.+", List.of(t0930_1130, t1300_1500), 0.25),
			build("IH", CFFEX, FUT, "IH[0-9]{3,4}@.+", List.of(t0930_1130, t1300_1500), 0.25),
			build("IF", CFFEX, FUT, "IF[0-9]{3,4}@.+", List.of(t0930_1130, t1300_1500), 0.25),
			build("IM", CFFEX, FUT, "IM[0-9]{3,4}@.+", List.of(t0930_1130, t1300_1500), 0.25),
			build("上证50", CFFEX, OPT, "HO[0-9]{3,4}[^@].+", List.of(t0930_1130, t1300_1500), 500),
			build("中证1000", CFFEX, OPT, "MO[0-9]{3,4}[^@].+", List.of(t0930_1130, t1300_1500), 500),
			build("沪深300", CFFEX, OPT, "IO[0-9]{3,4}[^@].+", List.of(t0930_1130, t1300_1500), 500),
			build("上证50etf", CFFEX, SPT, "HO[0-9]{3,4}[^@].+", List.of(t0930_1130, t1300_1500), 500),
			build("中证1000etf", CFFEX, SPT, "MO[0-9]{3,4}[^@].+", List.of(t0930_1130, t1300_1500), 500),
			build("沪深300etf", CFFEX, SPT, "IO[0-9]{3,4}[^@].+", List.of(t0930_1130, t1300_1500), 500),
			/* 国债品种 */
			build("三十债", CFFEX, FUT, "TL[0-9]{3,4}@.+", List.of(t0930_1130, t1330_1515), 400),
			build("十债", CFFEX, FUT, "T[0-9]{3,4}@.+", List.of(t0930_1130, t1330_1515), 400),
			build("五债", CFFEX, FUT, "TF[0-9]{3,4}@.+", List.of(t0930_1130, t1330_1515), 400),
			build("二债", CFFEX, FUT, "TS[0-9]{3,4}@.+", List.of(t0930_1130, t1330_1515), 400)
		);
	}
	
	private final TimeSlot t2100_2300 = TimeSlot.builder().start(DateTimeUtils.fromCacheTime(21, 00)).end(DateTimeUtils.fromCacheTime(23, 00)).build();
	private final TimeSlot t2100_0100 = TimeSlot.builder().start(DateTimeUtils.fromCacheTime(21, 00)).end(DateTimeUtils.fromCacheTime(1, 00)).build();
	private final TimeSlot t2100_0230 = TimeSlot.builder().start(DateTimeUtils.fromCacheTime(21, 00)).end(DateTimeUtils.fromCacheTime(2, 30)).build();
	
	private final TimeSlot t0900_1015 = TimeSlot.builder().start(DateTimeUtils.fromCacheTime(9, 00)).end(DateTimeUtils.fromCacheTime(10, 15)).build();
	private final TimeSlot t1030_1130 = TimeSlot.builder().start(DateTimeUtils.fromCacheTime(10, 30)).end(DateTimeUtils.fromCacheTime(11, 30)).build();
	private final TimeSlot t0930_1130 = TimeSlot.builder().start(DateTimeUtils.fromCacheTime(9, 30)).end(DateTimeUtils.fromCacheTime(11, 30)).build();
	private final TimeSlot t0900_1130 = TimeSlot.builder().start(DateTimeUtils.fromCacheTime(9, 00)).end(DateTimeUtils.fromCacheTime(11, 30)).build();
	
	private final TimeSlot t1300_1500 = TimeSlot.builder().start(DateTimeUtils.fromCacheTime(13, 00)).end(DateTimeUtils.fromCacheTime(15, 00)).build();
	private final TimeSlot t1330_1500 = TimeSlot.builder().start(DateTimeUtils.fromCacheTime(13, 30)).end(DateTimeUtils.fromCacheTime(15, 00)).build();
	private final TimeSlot t1330_1515 = TimeSlot.builder().start(DateTimeUtils.fromCacheTime(13, 30)).end(DateTimeUtils.fromCacheTime(15, 15)).build();
	
	private ContractDefinition build(String name, ExchangeEnum exchange, ProductClassEnum productClass, String pattern, 
			List<TimeSlot> times, double commissionInBP){
		TradeTimeDefinition tradeTimeDef = TradeTimeDefinition.builder().timeSlots(times).build();
		return ContractDefinition.builder().name(name).exchange(exchange).productClass(productClass).dataSource(dataSource)
				.symbolPattern(Pattern.compile(pattern)).tradeTimeDef(tradeTimeDef).commissionRate(commissionInBP / 10000D).build();
	}
	
	private ContractDefinition build(String name, ExchangeEnum exchange, ProductClassEnum productClass, String pattern, 
			List<TimeSlot> times, int commissionInCent){
		TradeTimeDefinition tradeTimeDef = TradeTimeDefinition.builder().timeSlots(times).build();
		return ContractDefinition.builder().name(name).exchange(exchange).productClass(productClass).dataSource(dataSource)
				.symbolPattern(Pattern.compile(pattern)).tradeTimeDef(tradeTimeDef).commissionFee(commissionInCent / 100D).build();
	}
}
