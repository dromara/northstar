package tech.quantit.northstar.strategy.api;

import xyz.redtorch.pb.CoreField.BarField;

/**
 * 复合行情BAR监听
 * @author KevinHuangwl
 *
 */
public interface MergedBarListener {

	void onMergedBar(BarField bar);
}
