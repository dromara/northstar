package org.dromara.northstar.main;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.strategy.api.annotation.StrategicComponent;

@Slf4j
public class ExternalJarClassLoader extends URLClassLoader {

	// 属于本类加载器加载的jar包
	private JarFile jarFile;

	// 保存已经加载过的Class对象
	private Map<String, Class<?>> cacheClassMap = new HashMap<>();

	// 保存本类加载器加载的class字节码
	private Map<String, byte[]> classBytesMap = new HashMap<>();

	// 需要注册的spring bean的name集合
	private List<String> registeredBean = new ArrayList<>();

	// 构造
	public ExternalJarClassLoader(URL[] urls, ClassLoader parent) {
	        super(urls, parent);
	        URL url = urls[0];
	        String path = url.getPath();
	        try {
	            jarFile = new JarFile(path);
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	        //初始化类加载器执行类加载
	        init();
	    }

	// 重写loadClass方法
	// 改写loadClass方式
	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		if (findLoadedClass(name) == null) {
			return super.loadClass(name);
		} else {
			return cacheClassMap.get(name);
		}
	}

	/**
	 * 方法描述 初始化类加载器，保存字节码
	 * 
	 * @method init
	 */
	private void init() {
		// 解析jar包每一项
		Enumeration<JarEntry> en = jarFile.entries();
		while (en.hasMoreElements()) {
			JarEntry je = en.nextElement();
			String name = je.getName();
			try(InputStream input = jarFile.getInputStream(je)){
				// 这里添加了路径扫描限制
				if (name.endsWith(".class")) {
					log.debug("加载扩展包中的类：{}", name);
					String className = name.replace(".class", "").replace("/", ".");
					
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					int bufferSize = 4096;
					byte[] buffer = new byte[bufferSize];
					int bytesNumRead = 0;
					while ((bytesNumRead = input.read(buffer)) != -1) {
						baos.write(buffer, 0, bytesNumRead);
					}
					byte[] classBytes = baos.toByteArray();
					classBytesMap.put(className, classBytes);
				}
			} catch (IOException e) {
				log.error("类加载异常", e);
			}
		}
		// 将jar中的每一个class字节码进行Class载入
		for (Map.Entry<String, byte[]> entry : classBytesMap.entrySet()) {
			String key = entry.getKey();
			Class<?> aClass = null;
			try {
				aClass = loadClass(key);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			cacheClassMap.put(key, aClass);
		}
	}
	
	/**
	 * 方法描述 初始化spring bean
	 * 
	 * @method initBean
	 */
	public void initBean() {
		SpringContextUtil.getBeanFactory().setBeanClassLoader(this);
		for (Map.Entry<String, Class<?>> entry : cacheClassMap.entrySet()) {
			String className = entry.getKey();
			Class<?> cla = entry.getValue();
			if (isSpringBeanClass(cla)) {
				BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
						.genericBeanDefinition(cla)
						.setScope(BeanDefinition.SCOPE_SINGLETON);
				
				// 将变量首字母置小写
				String beanName = StringUtils.uncapitalize(className);
				beanName = beanName.substring(beanName.lastIndexOf(".") + 1);
				beanName = StringUtils.uncapitalize(beanName);
				SpringContextUtil.getBeanFactory().registerBeanDefinition(beanName, beanDefinitionBuilder.getBeanDefinition());
				registeredBean.add(beanName);
				SpringContextUtil.getBeanFactory().getBean(beanName);
			}
		}
	}

	// 获取当前类加载器注册的bean
	// 在移除当前类加载器的时候需要手动删除这些注册的bean
	public List<String> getRegisteredBean() {
		return registeredBean;
	}

	/**
	 * 方法描述 判断class对象是否带有spring的注解
	 * 
	 * @method isSpringBeanClass
	 * @param cla jar中的每一个class
	 * @return true 是spring bean false 不是spring bean
	 */
	public boolean isSpringBeanClass(Class<?> cla) {
		if (cla == null) {
			return false;
		}
		// 是否是接口
		if (cla.isInterface()) {
			return false;
		}

		// 是否是抽象类
		if (Modifier.isAbstract(cla.getModifiers())) {
			return false;
		}
		
		return cla.getAnnotation(Component.class) != null 
				|| cla.getAnnotation(Repository.class) != null
				|| cla.getAnnotation(Service.class) != null
				|| cla.getAnnotation(StrategicComponent.class) != null;
	}

}
