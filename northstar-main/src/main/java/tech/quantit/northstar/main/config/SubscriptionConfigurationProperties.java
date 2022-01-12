package tech.quantit.northstar.main.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import lombok.Data;
import tech.quantit.northstar.main.YamlAndPropertySourceFactory;

@Data
@Configuration
@PropertySource(value = "classpath:contracts.yml", factory = YamlAndPropertySourceFactory.class)
public class SubscriptionConfigurationProperties {

	@Value("${northstar.subscription.ctp.classType.whitelist:}")
	private String clzTypeWhtlist;
	@Value("${northstar.subscription.ctp.classType.blacklist:}")
	private String clzTypeBlklist;
	@Value("${northstar.subscription.ctp.unifiedSymbol.whitelist:}")
	private String symbolWhtlist;
	@Value("${northstar.subscription.ctp.unifiedSymbol.blacklist:}")
	private String symbolBlklist;
}
