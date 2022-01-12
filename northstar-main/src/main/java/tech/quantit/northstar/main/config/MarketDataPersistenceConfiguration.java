package tech.quantit.northstar.main.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import lombok.Data;
import tech.quantit.northstar.main.YamlAndPropertySourceFactory;

@Configuration
@Data
@PropertySource(value = "classpath:contracts.yml", factory = YamlAndPropertySourceFactory.class)
@ConfigurationProperties(prefix = "northstar")
public class MarketDataPersistenceConfiguration {

	private List<String> allowPersistence;
	
}
