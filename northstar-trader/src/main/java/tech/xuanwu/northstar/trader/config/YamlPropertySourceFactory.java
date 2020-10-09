package tech.xuanwu.northstar.trader.config;

import java.io.IOException;
import java.util.List;

import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.DefaultPropertySourceFactory;
import org.springframework.core.io.support.EncodedResource;

public class YamlPropertySourceFactory extends DefaultPropertySourceFactory{

	@Override
	public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
		if (resource == null){
            return super.createPropertySource(name, resource);
        }
		List<PropertySource<?>> source = new YamlPropertySourceLoader().load(resource.getResource().getFilename(), resource.getResource());
        return source.get(0);
	}
	
}
