package com.dji.sample.center.app;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class AppContext implements ApplicationContextAware {
	private static ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		AppContext.applicationContext = applicationContext;
	}

	public static ApplicationContext getApplicationContext() {
		assertApplicationContext();
		return applicationContext;
	}

	public static <T> T getBean(String beanName) {
		assertApplicationContext();
		return (T) applicationContext.getBean(beanName);
	}

	public static <T> T getBean(Class<T> requiredType) {
		assertApplicationContext();
		return (T) applicationContext.getBean(requiredType);
	}

	private static void assertApplicationContext() {
		if (applicationContext == null) {
			throw new RuntimeException("applicaitonContext属性为null,请检查是否注入了SpringContextHolder!");
		}
	}
}
