package tech.xuanwu.northstar.trader.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Slf4j
@Configuration
@EnableSwagger2
public class SwaggerConfig {
	
	@Value("${spring.profiles.active}")
	private String profile;
	
    @Bean
    public Docket createRestApi() {
    	log.info("启动swagger");
        return new Docket(DocumentationType.SWAGGER_2)
//        		.enable(!StringUtils.equals(profile, "prod"))
                .pathMapping("/")
                .select()
                .apis(RequestHandlerSelectors.basePackage("tech.xuanwu.northstar.trader.controller"))
                .paths(PathSelectors.any())
                .build().apiInfo(new ApiInfoBuilder()
                        .title("Northstar监控端API")
                        .version("1.0")
                        .contact(new Contact("黄伟亮","https://www.zhihu.com/people/kevinbauer","12959229@qq.com"))
                        .license("The Apache License")
                        .licenseUrl("http://www.baidu.com")
                        .build());
    }
}
