package org.dromara.northstar.rl.reward;

import xyz.redtorch.pb.CoreField.BarField;

public abstract class RLReward {
	public static double rewardGenerator(BarField bar, BarField lastBar, int actionID, String rewardType) {
		switch (rewardType) {
			case "dummy" -> { return dummyReward(bar, actionID); }
			case "openDiff" -> { return openDiffReward(bar, lastBar, actionID); }
			default -> { return 0; }
		}
	}

	private static double dummyReward(BarField bar, int actionID) {
		return 0;
	}

	private static double openDiffReward(BarField bar, BarField lastBar, int actionID) {
		
		return bar.getOpenPrice() - lastBar.getOpenPrice();
	}
}