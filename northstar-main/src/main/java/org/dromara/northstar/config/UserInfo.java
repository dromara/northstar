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

	public UserInfo() {
		String user = System.getenv(Constants.NS_USER);
		String pwd = System.getenv(Constants.NS_PWD);
		userId = Optional.ofNullable(user).orElse(Constants.DEFAULT_USERID);
		password = Optional.ofNullable(pwd).orElse(Constants.DEFAULT_PASSWORD);	
	}
}
