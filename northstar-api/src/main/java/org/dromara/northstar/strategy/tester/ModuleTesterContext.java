package org.dromara.northstar.strategy.tester;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dromara.northstar.common.constant.PlaybackPrecision;
import org.dromara.northstar.common.constant.PlaybackSpeed;

/**
 * 模组自动化测试上下文
 * @author KevinHuangwl
 *
 */
public interface ModuleTesterContext {
	
	/**
	 * 预热起始日
	 * @return
	 */
	default LocalDate preStartDate() {
		return startDate().minusWeeks(20);
	}
	
	/**
	 * 测试开始日期
	 * @return
	 */
	default LocalDate startDate() {
		return endDate().minusMonths(24);
	}
	
	/**
	 * 测试结束日期
	 * @return
	 */
	default LocalDate endDate() {
		return LocalDate.now();
	}
	
	/**
	 * 回测精度
	 * @return
	 */
	default PlaybackPrecision precision() {
		return PlaybackPrecision.LOW;
	}
	
	/**
	 * 回测速度
	 * @return
	 */
	default PlaybackSpeed speed() {
		return PlaybackSpeed.RUSH;
	}
	
	/**
	 * 测试合约列表
	 * @return
	 */
	default List<String> testSymbols() {
		return List.of(
				"MA","AP","CF","SR","TA","SA","FG","OI","RM","UR",
				"rb","hc","ni","ru","cu","sn","al","zn","ag","au","bu","sp","fu",
				"m","y","eg","pp","i","jm","j","p","v","eb","l","lh","jd",
				"IC","IF","IH","IM");
	}
	
	/**
	 * 测试合约初始金额设置
	 * @return
	 */
	default Map<String, Integer> symbolTestAmount(){
		return new HashMap<>() {
			private static final long serialVersionUID = 1L;

			{
				put("MA", 10000);
				put("AP", 20000);
				put("CF", 20000);
				put("SR", 20000);
				put("TA", 10000);
				put("SA", 10000);
				put("FG", 10000);
				put("OI", 20000);
				put("RM", 10000);
				put("UR", 10000);
				put("rb", 10000);
				put("hc", 10000);
				put("ni", 30000);
				put("ru", 20000);
				put("cu", 50000);
				put("sn", 40000);
				put("al", 20000);
				put("zn", 20000);
				put("ag", 10000);
				put("au", 70000);
				put("bu", 10000);
				put("sp", 10000);
				put("fu", 10000);
				put("m", 10000);
				put("y", 15000);
				put("eg", 10000);
				put("pp", 10000);
				put("i", 20000);
				put("jm", 30000);
				put("j", 60000);
				put("p", 20000);
				put("v", 10000);
				put("eb", 10000);
				put("l", 10000);
				put("lh", 60000);
				put("jd", 10000);
				put("IC", 200000);
				put("IF", 200000);
				put("IH", 200000);
				put("IM", 200000);
				put("si", 10000);
			}
		};
	}
}
