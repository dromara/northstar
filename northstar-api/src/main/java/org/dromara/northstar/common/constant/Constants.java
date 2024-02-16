package org.dromara.northstar.common.constant;

public interface Constants {
	
	Double ZERO_D = 0D;
	
	Integer ZERO = 0;
	
	String INDEX_SUFFIX = "0000";
	
	String PRIMARY_SUFFIX = "9999";
	
	String MOCK_ORDER_ID = "MockOrderId";
	
	String OPTION_CHAIN_PREFIX = "OCC#";	// 期权链前缀
	
	String KEY_USER = "USER";
	
	String APP_NAME ="NS:";
	
	// 监控台默认登录信息
	String DEFAULT_USERID = "admin";
	String DEFAULT_PASSWORD = "123456";
	
	// 环境变量名
	String NS_DS_SECRET = "NS_DS_SECRET";
	String NS_USER = "NS_USER";
	String NS_PWD = "NS_PWD";
	
	// 资金占用比例估算系数
	double ESTIMATED_FROZEN_FACTOR = 1.5;
	
}
