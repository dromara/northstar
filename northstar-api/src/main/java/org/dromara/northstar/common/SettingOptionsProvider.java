package org.dromara.northstar.common;

import java.util.List;

import org.dromara.northstar.common.model.OptionItem;

public interface SettingOptionsProvider {

	/**
	 * 提供选项列表值
	 * @return
	 */
	List<OptionItem> optionVals();
}
