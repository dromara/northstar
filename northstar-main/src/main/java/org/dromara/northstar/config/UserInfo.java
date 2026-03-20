package org.dromara.northstar.config;

import java.util.Optional;

import org.dromara.northstar.common.constant.Constants;

import lombok.Getter;

/**
 * 用户信息
 * @author KevinHuangwl
 *
 */
@Getter
public class UserInfo {
	
	protected String userId;
	protected String password;
	
	private int errorLoginAttempt;

	public UserInfo() {
		String user = System.getenv(Constants.NS_USER);
		String pwd = System.getenv(Constants.NS_PWD);
		String user0 = System.getProperty("monitor.user", user);
		String pwd0 = System.getProperty("monitor.pwd", pwd);
		userId = Optional.ofNullable(user0).orElse(Constants.DEFAULT_USERID);
		password = Optional.ofNullable(pwd0).orElse(Constants.DEFAULT_PASSWORD);	
	}
	
	public void errorAttempt() {
		errorLoginAttempt++;
	}
	
	public void resetAttempt() {
		errorLoginAttempt = 0;
	}
	
	public boolean isLocked() {
		return errorLoginAttempt >= 3;
	}
}
