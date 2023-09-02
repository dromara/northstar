package org.dromara.northstar.strategy.trainer;

import java.time.LocalDate;

import org.dromara.northstar.common.model.ModuleAccountRuntimeDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.strategy.tester.ModuleTesterContext;

import cn.hutool.core.lang.Assert;

public interface RLAgentTrainingContext extends ModuleTesterContext {

	/**
	 * 最大训练回合数
	 * @return
	 */
	default int maxTrainingEpisodes() {
		return 1000;
	}
	
	/**
	 * 模组绩效是否收敛
	 * @param mrd1
	 * @param mrd2
	 * @return
	 */
	default boolean hasPerformanceConverged(ModuleRuntimeDescription mrd1, ModuleRuntimeDescription mrd2) {
		Assert.isTrue(mrd1.getModuleName().equals(mrd2.getModuleName()), "两模组运行时对象不一致");
		ModuleAccountRuntimeDescription mard1 = mrd1.getAccountRuntimeDescription();
		ModuleAccountRuntimeDescription mard2 = mrd2.getAccountRuntimeDescription();
		return CommonUtils.isEquals(mard1.getAccCloseProfit(), mard2.getAccCloseProfit()) 
				&& mard1.getAccDealVolume() == mard2.getAccDealVolume();
	}
	
	/**
	 * 预热起始日
	 * @return
	 */
	@Override
	default LocalDate preStartDate() {
		return LocalDate.of(2020, 5, 1);
	}
	
	/**
	 * 训练开始日期
	 * @return
	 */
	@Override
	default LocalDate startDate() {
		return LocalDate.of(2021, 1, 1);
	}
	
	/**
	 * 测试结束日期
	 * @return
	 */
	@Override
	default LocalDate endDate() {
		return LocalDate.of(2023, 1, 1);
	}
}
