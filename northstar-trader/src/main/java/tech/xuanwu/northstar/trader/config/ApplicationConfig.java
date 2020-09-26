package tech.xuanwu.northstar.trader.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.mongodb.MongoClient;

import tech.xuanwu.northstar.utils.MongoDBClient;

@Configuration
public class ApplicationConfig implements WebMvcConfigurer{
	
	
	@Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("POST", "GET", "PUT", "OPTIONS", "DELETE")
                .maxAge(3600)
                .allowCredentials(true);
    }
	
	@Bean
	public MongoDBClient createMongo(MongoClient client) {
		return new MongoDBClient(client);
	}
}
