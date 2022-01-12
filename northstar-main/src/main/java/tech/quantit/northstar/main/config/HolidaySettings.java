package tech.quantit.northstar.main.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import lombok.Data;
import tech.quantit.northstar.main.YamlAndPropertySourceFactory;

@Data
@Configuration
@ConfigurationProperties(prefix = "northstar")
@PropertySource(value = "classpath:holidays.yml", factory = YamlAndPropertySourceFactory.class)
public class HolidaySettings {

	private List<String> holidays;
}
