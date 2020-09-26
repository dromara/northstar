package tech.xuanwu.northstar.strategy.config.strategy;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="strategy-setting.shadow-strategy")
public class ShadowStrategyConfig extends BaseStrategyConfig{
	
	private String followAccount;

	public String getFollowAccount() {
		return followAccount;
	}

	public void setFollowAccount(String followAccount) {
		this.followAccount = followAccount;
	}
	
}
