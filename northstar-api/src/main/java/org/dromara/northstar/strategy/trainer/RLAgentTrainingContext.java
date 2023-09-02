package org.dromara.northstar.strategy.trainer;

import java.time.LocalDate;

import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.strategy.tester.ModuleTesterContext;

public interface RLAgentTrainingContext extends ModuleTesterContext {

	default int maxTrainingEpisodes() {
		return 1000;
	}
	
	boolean hasConverged(ModuleRuntimeDescription d1, ModuleRuntimeDescription d2);
	
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
