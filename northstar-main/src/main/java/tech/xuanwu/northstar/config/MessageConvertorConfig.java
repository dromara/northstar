package tech.xuanwu.northstar.config;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 配置转换器
 * @author KevinHuangwl
 *
 */
@Configuration
public class MessageConvertorConfig implements WebMvcConfigurer {

	@Override
	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
		/**
		 * 调整转换器优先级
		 */
		List<HttpMessageConverter<?>> jacksonConverters = new ArrayList<>();
		Iterator<HttpMessageConverter<?>> itCvt = converters.iterator();
		while(itCvt.hasNext()) {
			HttpMessageConverter<?> cvt = itCvt.next();
			if(cvt instanceof MappingJackson2HttpMessageConverter) {
				jacksonConverters.add(cvt);
				itCvt.remove();
			}
		}
		for(HttpMessageConverter<?> cvt : jacksonConverters) {
			converters.add(0, cvt);
		}
	}

	
}
